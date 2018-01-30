(ns coordinate-four
  (:require [clojure.core.matrix :as m
             :refer [mul mmul add sub div]])
  (:use [anglican core emit runtime
         [inference :refer [infer equalize]]
         [state :refer [get-predicts get-log-weight]]]))

(defn valid-weight? [state]
  (> (get-log-weight state) Double/NEGATIVE_INFINITY))

;;; Slightly different "conditional" definition: filters out -Inf
(defn conditional [query & options]
  (let [[algorithm & options] options
        algorithm (or algorithm :importance)]
    (fn [& value]
      (let [source (-> (filter valid-weight?
                               (apply infer algorithm query value options))
                       equalize
                       ref)
            next-sample #(dosync
                          (let [samples @source]
                            (ref-set source (rest samples))
                            (first samples)))]
        (reify distribution
          (sample [this]
            (get-predicts (next-sample)))
          (observe [this value]
            (throw (Exception. "not implemented"))))))))


;;; Amy and Bob are hoping to meet up. Pub or starbucks?

(defdist location [pub] []
  (sample [this] (if (sample (flip pub)) :pub :starbucks))
  (observe [this value] (observe (flip pub) (= value :pub))))

(sample (location 0.6))

;;; Schilling coordination game

(declare amy bob amy-dist bob-dist)

(with-primitive-procedures [location amy-dist bob-dist]
  (defquery amy [depth]
    ;;(println "[amy] start of query, depth is" depth)
    (let [amy-preference (location 0.6)
          ;;amy-location (sample amy-preference)
          ;;_ (println "[amy] about to sample" depth)
          bob-location (:bob (sample (bob-dist (- depth 1))))]
      ;;(observe-constraint (= amy-location bob-location))
      (observe amy-preference bob-location)
      (predict :amy bob-location)))

  (defquery bob [depth]
    ;;(println "[bob] start of query, depth is" depth)
    (let [bob-preference (location 0.6) ;; if depth = 0, 1.0
          bob-location (sample bob-preference)]
      (if (> depth 0)
        (let [;;_ (println "[bob] about to sample" depth)
              amy-location (:amy (sample (amy-dist depth)))]
          (observe bob-preference amy-location)
          (predict :bob amy-location))
        (predict :bob bob-location)))))

(def amy-dist (conditional amy))
(def bob-dist (conditional bob))

(defn coordinate [meta-reasoning-depth]
  (let [N 100
        amy-dist ((conditional amy :lmh) meta-reasoning-depth)
        bob-dist ((conditional bob) meta-reasoning-depth)
        amy-samples (repeatedly N #(sample amy-dist))
        bob-samples (repeatedly N #(sample bob-dist))
        pub-probability (fn [outcomes] (/ (count (filter #(= :pub %) outcomes)) (float N)))]
    (println "recursion depth: " meta-reasoning-depth)
    (println "p(Amy at pub): " (pub-probability (map :amy amy-samples)))
    (println "p(Bob at pub): " (pub-probability (map :bob bob-samples)))))

;;; At a recursion depth of zero, Bob just picks at random, so is at the pub with probabilty 0.6.
;;; Amy knows this, so is more likely to go to the pub herself.
(def depth 0)
(time (coordinate depth))

;;; At a recursion depth of 1, Bob also reasons about what Amy will do.
;;; Bob is now much more likely to go to the pub, too.
(def depth 1)
(time (coordinate depth))

(def depth 2)
(time (coordinate depth))

;;; At higher recursion depths, they both become more likely to go to the pub.
;;; At depth 3, this is > 90% for both of them.
(def depth 3)
(time (coordinate depth))

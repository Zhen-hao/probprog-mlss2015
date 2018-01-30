(ns coordinate-two
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
                          (let [[sample & samples] @source]
                            (ref-set source samples)
                            sample))]
        (reify distribution
          (sample [this]
            (get-predicts (next-sample)))
          (observe [this value]
            (throw (Exception. "not implemented"))))))))

;; Shortcut / helper for observing hard constraints
(defm observe-constraint [truth-value]
  (observe (flip 1.0) truth-value))


;;; Amy and Bob are hoping to meet up. Pub or starbucks?
(defm sample-location
  "go to the pub, or go to starbucks?"
  []
  (if (sample (flip 0.60)) :pub :starbucks))


;;; Schilling coordination game

(declare amy)
(declare bob)


(defn amy-dist [depth]
  ((conditional (query [] (predict :amy (amy depth))))))

(defn bob-dist [depth]
  ((conditional (query [] (predict :bob (bob depth))))))

(with-primitive-procedures [bob-dist amy-dist]
  (defm amy
    "where does amy go?"
    [depth]
    (let [amy-location (sample-location)
          bob-location (:bob (sample (bob-dist (- depth 1))))]
      (observe-constraint (= amy-location bob-location))
      amy-location))

  (defm bob
    "where does bob go?"
    [depth]
    (let [bob-location (sample-location)]
      (when (> depth 0)
        (let [amy-location (:amy (sample (amy-dist depth)))]
          (observe-constraint (= bob-location amy-location))))
      bob-location)))


(defn coordinate [meta-reasoning-depth]
  (let [N 1000
        amy-dist (amy-dist meta-reasoning-depth)
        bob-dist (bob-dist meta-reasoning-depth)
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

;;; At higher recursion depths, they both become more likely to go to the pub.
;;; At depth 3, this is > 90% for both of them.
(def depth 3)
(time (coordinate depth))

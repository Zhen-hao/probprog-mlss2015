(ns coordinate-three
  (:require [clojure.core.matrix :as m
             :refer [mul mmul add sub div]])
  (:use [anglican core emit runtime
         [inference :refer [infer equalize]]
         [state :refer [get-predicts get-log-weight]]]))

(defn valid-weight? [state]
  (> (get-log-weight state) Double/NEGATIVE_INFINITY))

;; Shortcut / helper for observing hard constraints

(defm observe-constraint [truth-value]
  (observe (flip 1.0) truth-value))

;;; Simpler alternative to "conditional"
(defn rejection-query [q args]
  (get-predicts (first (filter valid-weight? (doquery :importance q args)))))


;;; Amy and Bob are hoping to meet up. Pub or starbucks?

(defm sample-location
  "go to the pub, or go to starbucks?"
  []
  (if (sample (flip 0.60)) :pub :starbucks))


;;; Schilling coordination game

(declare amy)
(declare bob)

(with-primitive-procedures [rejection-query]
  (defquery amy [depth]
  ;;  (println "[amy] start of query, depth is" depth)
    (let [amy-location (sample-location)
    ;;      _ (println "[amy] about to sample" depth)
          bob-location (:bob (rejection-query bob [(- depth 1)]))]
      (observe-constraint (= amy-location bob-location))
      (predict :amy amy-location)))

  (defquery bob [depth]
   ;; (println "[bob] start of query, depth is" depth)
    (let [bob-location (sample-location)]
      (when (> depth 0)
        (let [;;_ (println "[bob] about to sample" depth)
              amy-location (:amy (rejection-query amy [depth]))]
          (observe-constraint (= bob-location amy-location))))
      (predict :bob bob-location))))

(defn coordinate [meta-reasoning-depth]
  (let [N 1000
        amy-samples (repeatedly N #(rejection-query amy [meta-reasoning-depth]))
        bob-samples (repeatedly N #(rejection-query bob [meta-reasoning-depth]))
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

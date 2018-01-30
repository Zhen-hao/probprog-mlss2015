(ns coordinate-one
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

(defquery amys-inner-query [depth]
  (let [amy-location (sample-location)
        bob-location (bob (- depth 1))]
    (observe-constraint (= amy-location bob-location))
    (predict :location amy-location)))

(defquery bobs-inner-query [depth]
  (let [bob-location (sample-location)
        amy-location (amy depth)]
    (observe-constraint (= bob-location amy-location))
    (predict :location bob-location)))

(with-primitive-procedures [rejection-query]
  (defm amy
    "where does amy go?"
    [depth]
    (:location (rejection-query amys-inner-query [depth])))

  (defm bob
    "where does bob go?"
    [depth]
    (if (<= depth 0)
      ;;; Bob is tired. His head hurts, and he just wants to pick a place.
      (sample-location)
      (:location (rejection-query bobs-inner-query [depth])))))


(defn coordinate [meta-reasoning-depth]
  (let [N 1000
        amy-samples
        (repeatedly N
                    #(rejection-query
                      (query [] (predict :amy (amy meta-reasoning-depth))) []))
        bob-samples
        (repeatedly N
                    #(rejection-query
                      (query [] (predict :bob (bob meta-reasoning-depth))) []))
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

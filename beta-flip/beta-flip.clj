;; Probabilistic programming inference
;; in a Beta-Bernoulli model, this example
;; illustrates different ways of
;; writing queries and different ways
;; of consuming their output
;;
;; Frank Wood fwood@robots.ox.ac.uk

(ns beta-flip
  (:require [anglican.importance] [anglican.stat :as stat])
  (:use [anglican
           [core :exclude [-main]]
           runtime
           emit
           [inference :only [infer collect-by equalize]]
           [state :only [get-predicts get-log-weight set-log-weight]]]))

;; let's ask
;; that theta ~ beta(5,3)
;;      x = true ~ bernoulli(theta)
;; p(theta>0.7)
;;
;; Ground truth = p(theta>0.7) = .448 = 1-betacdf(6,3,0.7)
;;
;; defquery is sugar for
(defquery one-flip [outcome]
  (let [theta (sample (beta 5 3))]
    (observe (flip theta) outcome)
    (predict :theta-gt-pt07 (> theta 0.7))))

;; defining a query (alt syntax).  both defquery and query compile
;; the enclosed _Anglican program_ (which looks just
;; like Clojure but isn't)
(def one-flip (query [outcome]
  (let [theta (sample (beta 5 3))]
    (observe (flip theta) outcome)
    (predict :theta-gt-pt07 (> theta 0.7)))))

;; conditional constructs a distribution object constructor
;; given a query specification of a conditional distribution
;; under the hood conditional
;; "remembers" a stateful lazy weighted sample sequence generator and
;; equalizes the sample stream, effectively throwing away the
;; importance weights -- this is statistically inefficient but syntactically simple
(def one-flip-posterior (conditional one-flip))

;; given the conditional constructor
;; we can construct a sample-able (but not observable)
;; distribution object with the condition passed as an argument
(def true-flip-posterior (one-flip-posterior true))

;; sampling repeatedly from this distribution object
;; characterizes the distribution
(frequencies (repeatedly 100000
                         #(sample true-flip-posterior)))

;; if you are familiar with sampling techniques both
;; in general and in Anglican you may
;; directly interact with the sampler output, bearing in mind
;; that some samplers return _weighted samples_ which
;; should be accounted for in subsequent use and some will
;; return samples with weight -Inf indicating that not all
;; constraints have been satisfied yet; "conditional"
;; does this and more under the covers
(def one-flip->sample
  (->> (doquery :lmh one-flip [true] :number-of-particles 100)
       (filter #(> (:log-weight % ) Double/NEGATIVE_INFINITY))
       (map get-predicts)
       (map :theta-gt-pt07)))
(frequencies (take 10000 one-flip->sample))

;; otherwise you may use helper functions that collate
;; sampler output in a way that properly uses the weights,
;; retaining statistical effeciency but requiring you
;; to know about and use the helper functions
(->> (doquery :smc one-flip [true] :number-of-particles 100)
     (take 10000)
     (collect-by :theta-gt-pt07)
     (stat/empirical-distribution))


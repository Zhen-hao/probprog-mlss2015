;; gorilla-repl.fileformat = 1

;; **
;;; # Gaussian posterior estimation
;; **

;; @@
(ns gaussian-estimation
  (:require [anglican importance pgibbs]
            [anglican.stat :as stat]
            [gorilla-plot.core :as plot])
  (:use [anglican core emit runtime]))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; We can use Anglican for posterior estimation in Bayesian models.
;;; 
;;; Suppose we are trying to estimate the mean of a Gaussian distribution, given some observed data @@y\_i@@.
;;; We'll assume that the variance is known, and focus on learning the posterior distribution of the mean @@\mu@@.
;;; We put a Gaussian prior on @@\mu@@, yielding a model:
;;; 
;;; $$\begin{align}
;;; \sigma^2 &= 2 \\\\
;;; \mu &\sim \mathrm{Normal}(1, \sqrt 5) \\\\
;;; y\_i|\mu &\sim \mathrm{Normal}(\mu, \sigma).
;;; \end{align}$$
;;; 
;;; Now suppose we observe two data points, @@y\_1 = 9@@ and @@y\_2 = 8@@.
;;; This will be passed as an input to the query as a vector with two elements.
;; **

;; @@
(def dataset [9 8])
;; @@

;; **
;;; Write this model as a simple Anglican program:
;; **

;; @@
(defquery gaussian-model [data]

  ;;; *** YOUR CODE HERE ***

  ;;; *** change the following line too ***
  (predict :mu 1.0))
;; @@

;; **
;;; The following code creates a distribution object which represents the posterior distribution, conditioned on the supplied `dataset`, using the particle Gibbs inference algorithm.
;; **

;; @@
(def posterior 
  ((conditional gaussian-model :pgibbs :number-of-particles 1000) dataset))
;; @@

;; **
;;; The following line now draws 20,000 samples from the posterior distribution.
;; **

;; @@
(def posterior-samples (repeatedly 20000 #(sample posterior)))
;; @@

;; **
;;; We can plot a histogram of these samples to see the posterior.
;;; 
;;; Here we have chosen the [conjugate prior](http://en.wikipedia.org/wiki/Conjugate_prior) for @@\mu@@, making this a rare model in that we can actually compute the posterior distribution analytically
;;; &mdash; when we run our sampler, we expect to find
;;; 
;;; $$\begin{align}
;;; \mu|y\_{1:2} &\sim \mathrm{Normal}(7.25, 0.9129).
;;; \end{align}$$
;;; 
;;; We can also draw samples from the prior distribution @@\mathrm{Normal}(1,\sqrt 5)@@, to see how the posterior differs from the prior:
;; **

;; @@
(def prior-samples (repeatedly 20000 #(sample (normal 1 (sqrt 5)))))


(println "Prior on mu (blue) and posterior (green)")
(plot/compose
 (plot/histogram prior-samples
                 :normalize :probability-density :bins 40
                 :plot-range [[-10 10] [0 0.8]])
 (plot/histogram (map :mu posterior-samples)
                 :normalize :probability-density :bins 40
                 :color :green))
;; @@

;; **
;;; # The seven scientists
;;; 
;;; Here's an interesting variation on estimating the mean of a Gaussian. This example is from [MacKay 2003, exercise 22.15] and [Lee & Wagenmaker 2013, section 4.2].
;;; 
;;; Suppose seven scientists all go and perform the same experiment, each collecting a measurement @@x\_i@@ for @@i = 1,\dots,7@@. 
;;; 
;;; These scientists are varyingly good at their job, and while we can assume each scientist would estimate @@x@@ correctly _on average_, some of them may have much more error in their measurements than others.
;;; 
;;; They come back with the following seven observations:
;; **

;; @@
(def measurements [-27.020 3.570 8.191 9.898 9.603 9.945 10.056])

(plot/bar-chart (range 1 8) measurements)
;; @@

;; **
;;; Clearly scientist 1 does not know what he is doing (and 2 and 3 are probably a little suspect too)!
;;; 
;;; To model this situation, we place simple priors on the mean @@\mu@@ of the measurements, and the error standard deviation @@\sigma\_i@@ for each of the @@i@@ scientists.
;;; 
;;; As a starting point, consider placing uninformative priors on these parameters; a suggestion is:
;;; $$\begin{align}
;;; \mu &\sim \mathrm{Normal}(0, 50) \\\\
;;; \sigma\_i &\sim \mathrm{Uniform}(0, 25)
;;; \end{align}$$
;;; 
;;; The uniform distribution over real numbers on the open interval @@(a, b)@@ can be constructed in Anglican with `(uniform-continuous a b)`.
;;; 
;;; We can ask two questions here:
;;; 
;;; * Given these measurements, what is the posterior distribution of @@x@@?
;;; * What distribution over noise level @@\sigma_i@@ do we infer for each of these scientists' estimates?
;;; 
;;; Write the model in Anglican such that it has two `predict` statements, one for the value `:x` and one for the vector of seven noise levels `:noise`.
;; **

;; @@
(defquery scientists [measurements]
  
  ;;; *** YOUR CODE HERE ***
  
  ;;; *** modify the next two lines as needed ***
  (predict :x 0.0)
  (predict :noise (repeat 7 1.0)))
;; @@

;; **
;;; Given the measurements, we sample from the conditional distribution and plot a histogram of the results.
;; **

;; @@
(def scientist-posterior ((conditional scientists :pgibbs :number-of-particles 1000) measurements))

(def scientist-samples (repeatedly 50000 #(sample scientist-posterior)))
;; @@

;; @@
(println "Expected value of measured quantity:" (stat/mean (map :x scientist-samples)))

(plot/histogram (map :x scientist-samples)
                :normalize :probability
                :bins 20)
;; @@

;; @@
(def noise-estimate (stat/mean (map :noise scientist-samples)))

(plot/bar-chart (range 1 8) noise-estimate)
;; @@

;; **
;;; * Are these noise levels what you would expect?
;;; * How sensitive is this to the prior on @@\mu@@ and @@\sigma\_i@@?
;; **

;; **
;;; 
;; **
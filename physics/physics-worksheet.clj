;; gorilla-repl.fileformat = 1

;; **
;;; 
;; **

;; **
;;; 2D Physics
;;; ==========
;;; 
;;; Probabilistic programming inference through a complex nondifferentiable deterministic procedure, here a 2D physics engine; using inference to do mechanism design, namely positioning bumpers to safely deliver a number of bouncy balls into a waiting bin.
;;; 
;;; Authors:
;;; 
;;;  - Frank Wood [fwood@robots.ox.ac.uk](mailto:fwood@robots.ox.ac.uk)
;;;  - Brooks Paige [brooks@robots.ox.ac.uk](mailto:brooks@robots.ox.ac.uk)
;;; 
;;; Task
;;; ====
;;; 
;;; Run the code below to see a 2D "world" simulation.  Then write (modify) a query to infer bumper locations that get, ideally, all of the ten dropped balls into the bin.
;;; 
;;; The world simulation code itself is writen in a separate file, `src/bounce.clj`, which we load here. You don't need to look at the simulation code itself to perform the exercise, unless you are interested.
;; **

;; @@
(ns bounce-worksheet
  (:require [org.nfrac.cljbox2d.core :refer [position]])
  (:use [anglican [core :exclude [-main]]
           runtime emit [inference :only [infer]]
           [state :only [get-predicts get-log-weight set-log-weight]]]))

(require '[bounce :refer [create-world show-world-simulation 
                          simulate-world display-static-world
                          balls-in-box]] :reload)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; The task: a Rube-Goldberg device design task of sorts; given a ball emitter and a bin into which you would like the balls to end up, use inference to configure a set of bumpers such that the balls do end up in the bin.
;;; 
;;; Here's a purposefully bad guess as to where bumpers might go; the values used should also give you a sense of the dimensions of the world (for ease of reference, the bin is at [15 0], is 2 units wide and has walls of height 2, balls are dropped from around [-5 8.25] down a ramp that goes from [-6 9] to [-4 7]).
;;; 
;;; The bumper locations themselves are a list of pairs, where the pairs are the center of the bumpers.
;; **

;; @@
;; A bumper location example is a list of pairs.
;; Here as an example, we place 4 different bumpers:
(def bumper-location-example 
  (list [-3 6] [2 5] [7 4] [12 3]))

;; Create a world with given bumpers at these example locations:
(def example-world (create-world bumper-location-example))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;bounce-worksheet/example-world</span>","value":"#'bounce-worksheet/example-world"}
;; <=

;; **
;;; There are two ways of examining the "world" we just created. The most exciting is to play forward the balls bouncing, in real time, and see where they go! For this, we use the `show-world-simulation` function we provide.
;;; 
;;; When you run the following line, it will open up a Java applet window showing the simulation in progress. Note that the window might not open up "on top" of this browser window.
;;; 
;;; Also, note that every time you run this, a _new_ applet window will be created.
;; **

;; @@
;; and to see why you can run the simulator in "real time"
;; watching it as it goes
(show-world-simulation bumper-location-example)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>#object[quil.Applet 0x2599c632 &quot;quil.Applet[panel0,0,0,600x500,layout=java.awt.FlowLayout]&quot;]</span>","value":"#object[quil.Applet 0x2599c632 \"quil.Applet[panel0,0,0,600x500,layout=java.awt.FlowLayout]\"]"}
;; <=

;; **
;;; We don't need to simulate in real time -- here, the `simulate-world` function runs the simulation forward, dropping 10 balls, and recording their locations after 20 seconds.
;;; 
;;; We can look at their locations, and see where they end up (visually, `display-static-world` can be used for this). For the (provided) example locations, none of the balls end in the bucket.
;; **

;; @@
;; Simulate the final state using the 2d physics engine:
(def example-world-final-state (simulate-world example-world))

;; The ball positions can be examined by getting `:balls` from the final state:
(map position (:balls example-world-final-state))

;; Show the conclusion of simulation -- note
;; that you see no balls which suggests they have missed
;; the target:
(display-static-world bumper-location-example
                      (:balls example-world-final-state))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>#object[quil.Applet 0x22271b11 &quot;quil.Applet[panel4,0,0,600x500,layout=java.awt.FlowLayout]&quot;]</span>","value":"#object[quil.Applet 0x22271b11 \"quil.Applet[panel4,0,0,600x500,layout=java.awt.FlowLayout]\"]"}
;; <=

;; **
;;; ?## Inference
;;; 
;;; We provide a useful function (defined in `src/bounce.clj`) which counts the number of balls which are in the box:
;; **

;; @@
;; Just to check, our guess at the bumper locations
;; leaves none in the box:
(balls-in-box example-world-final-state)
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"}
;; <=

;; **
;;; Instead of painstakingly hand-tuning the bumper locations, let's use ABC-style inference to figure out where to place them.
;;; 
;;; To do this, we need to 
;;; 
;;; 1. define simulation code (aka, a "prior") which defines a distribution over the number of bumpers and their locations, and
;;; 2. define a likelihood (using `observe`) to penalize program executions (i.e. configurations of bumpers) which do not place balls in the bucket.
;;; 
;;; A skeleton of the query is here below:
;; **

;; @@
;; To do this we've provided scaffolding
;; that can be modified to achieve
;; your objective:
(with-primitive-procedures
  [create-world simulate-world balls-in-box]
  (defquery arrange-bumpers []
    (let [;; *** TASK: REPLACE THE FOLLOWING LINE WITH 
          ;;     STOCHASTIC CODE TO SAMPLE POSSIBLE 
          ;; 	 BUMPER CONFIGURATIONS ***
          n-bumpers 4 ;; (sample (poisson 4)) 
          bumper-positions (repeatedly n-bumpers #(list 
                                            (sample (uniform-continuous -5 16))
                                            (sample (uniform-continuous -20 8))))

          ;; Code to simulate the world:
          world (create-world bumper-positions)
          end-world (simulate-world world)
          balls (:balls end-world)

          ;; How many balls entered the box?
          num-balls-in-box (balls-in-box end-world)]

      ;; *** ADD A LIKELIHOOD FUNCTION (observe) HERE ***
      ;;(observe (normal n-bumpers 1) 0)
      (observe (normal num-balls-in-box 1) 10)
      
      (predict :balls balls)
      (predict :num-balls-in-box num-balls-in-box)
      (predict :bumper-positions bumper-positions))))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;bounce-worksheet/arrange-bumpers</span>","value":"#'bounce-worksheet/arrange-bumpers"}
;; <=

;; **
;;; Here is some basic inference code; we run lightweight metropolis-hastings for 1500 samples, and then take the last one. This should yield a sample which does reasonably well at the task:
;; **

;; @@
;; Here we'll use the doquery syntax because
;; we'd like to monitor convergence; we'll
;; use lightweight metropolis hastings to
;; perform the query:
(def sampler (doquery :lmh arrange-bumpers nil))

;; We will use a silly notion of what is "best," namely
;; the configuration learned after 1500 sweeps, i.e.
;; single variable proposed changes, effectively
;; counting on lmh to stochastically ascend the 
;; posterior but not mix (a reasonably safe bet here):
(def best-configuration (get-predicts (first (drop 1500 sampler))))

;; We can look at the best configuration:
best-configuration

;; And look at convergence with respect to the
;; number of balls left in the box:
(->> (take 1500 sampler)
     (take-nth 10)
     (map get-predicts)
     (map :num-balls-in-box))
;; @@
;; =>
;;; {"type":"list-like","open":"<span class='clj-lazy-seq'>(</span>","close":"<span class='clj-lazy-seq'>)</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"},{"type":"html","content":"<span class='clj-unkown'>10</span>","value":"10"}],"value":"(0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10)"}
;; <=

;; **
;;; Now, we can watch the simulation in action with our inferred world:
;; **

;; @@
;; This is what it looks like when run with 20 balls:
(show-world-simulation (:bumper-positions best-configuration))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-unkown'>#object[quil.Applet 0x3a4c217f &quot;quil.Applet[panel11,0,0,600x500,layout=java.awt.FlowLayout]&quot;]</span>","value":"#object[quil.Applet 0x3a4c217f \"quil.Applet[panel11,0,0,600x500,layout=java.awt.FlowLayout]\"]"}
;; <=

;; **
;;; Good luck!
;;; ==========
;; **

;; @@
(repeatedly 4 #(list 
                                            (sample (uniform-continuous -5 16))
                                            (sample (uniform-continuous -0.5 8))))
;; @@
;; =>
;;; {"type":"list-like","open":"<span class='clj-lazy-seq'>(</span>","close":"<span class='clj-lazy-seq'>)</span>","separator":" ","items":[{"type":"list-like","open":"<span class='clj-list'>(</span>","close":"<span class='clj-list'>)</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-double'>8.367053030757234</span>","value":"8.367053030757234"},{"type":"html","content":"<span class='clj-double'>4.623640604433604</span>","value":"4.623640604433604"}],"value":"(8.367053030757234 4.623640604433604)"},{"type":"list-like","open":"<span class='clj-list'>(</span>","close":"<span class='clj-list'>)</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-double'>11.690751360496506</span>","value":"11.690751360496506"},{"type":"html","content":"<span class='clj-double'>0.9827469917945564</span>","value":"0.9827469917945564"}],"value":"(11.690751360496506 0.9827469917945564)"},{"type":"list-like","open":"<span class='clj-list'>(</span>","close":"<span class='clj-list'>)</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-double'>12.248459907015786</span>","value":"12.248459907015786"},{"type":"html","content":"<span class='clj-double'>-0.12893980334047228</span>","value":"-0.12893980334047228"}],"value":"(12.248459907015786 -0.12893980334047228)"},{"type":"list-like","open":"<span class='clj-list'>(</span>","close":"<span class='clj-list'>)</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-double'>2.5219241697341204</span>","value":"2.5219241697341204"},{"type":"html","content":"<span class='clj-double'>-0.15903787349816412</span>","value":"-0.15903787349816412"}],"value":"(2.5219241697341204 -0.15903787349816412)"}],"value":"((8.367053030757234 4.623640604433604) (11.690751360496506 0.9827469917945564) (12.248459907015786 -0.12893980334047228) (2.5219241697341204 -0.15903787349816412))"}
;; <=

;; @@

(let
      [n-bumpers 8
       f (fn [] (list
           (sample (uniform-continuous -5 14))
           (sample (uniform-continuous 0 10))))
       bs (repeatedly n-bumpers f)
       w0 (create-world bs)
       w1 (simulate-world w0)
       num-balls (balls-in-box w1)]
      (observe (normal 20 1) num-balls)
      (list num-balls bs))
;; @@
;; =>
;;; {"type":"list-like","open":"<span class='clj-list'>(</span>","close":"<span class='clj-list'>)</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-unkown'>0</span>","value":"0"},{"type":"list-like","open":"<span class='clj-lazy-seq'>(</span>","close":"<span class='clj-lazy-seq'>)</span>","separator":" ","items":[{"type":"list-like","open":"<span class='clj-list'>(</span>","close":"<span class='clj-list'>)</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-double'>-3.605326461372897</span>","value":"-3.605326461372897"},{"type":"html","content":"<span class='clj-double'>8.426766698248684</span>","value":"8.426766698248684"}],"value":"(-3.605326461372897 8.426766698248684)"},{"type":"list-like","open":"<span class='clj-list'>(</span>","close":"<span class='clj-list'>)</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-double'>0.7944855261594057</span>","value":"0.7944855261594057"},{"type":"html","content":"<span class='clj-double'>9.90083156619221</span>","value":"9.90083156619221"}],"value":"(0.7944855261594057 9.90083156619221)"},{"type":"list-like","open":"<span class='clj-list'>(</span>","close":"<span class='clj-list'>)</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-double'>-0.3857196127064526</span>","value":"-0.3857196127064526"},{"type":"html","content":"<span class='clj-double'>5.327107526827604</span>","value":"5.327107526827604"}],"value":"(-0.3857196127064526 5.327107526827604)"},{"type":"list-like","open":"<span class='clj-list'>(</span>","close":"<span class='clj-list'>)</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-double'>0.6304292923305184</span>","value":"0.6304292923305184"},{"type":"html","content":"<span class='clj-double'>3.839924451895058</span>","value":"3.839924451895058"}],"value":"(0.6304292923305184 3.839924451895058)"},{"type":"list-like","open":"<span class='clj-list'>(</span>","close":"<span class='clj-list'>)</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-double'>6.398137726588175</span>","value":"6.398137726588175"},{"type":"html","content":"<span class='clj-double'>5.857059662230313</span>","value":"5.857059662230313"}],"value":"(6.398137726588175 5.857059662230313)"},{"type":"list-like","open":"<span class='clj-list'>(</span>","close":"<span class='clj-list'>)</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-double'>-0.45716292154975235</span>","value":"-0.45716292154975235"},{"type":"html","content":"<span class='clj-double'>1.0282586980611086</span>","value":"1.0282586980611086"}],"value":"(-0.45716292154975235 1.0282586980611086)"},{"type":"list-like","open":"<span class='clj-list'>(</span>","close":"<span class='clj-list'>)</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-double'>-3.726910886587575</span>","value":"-3.726910886587575"},{"type":"html","content":"<span class='clj-double'>7.831128865946084</span>","value":"7.831128865946084"}],"value":"(-3.726910886587575 7.831128865946084)"},{"type":"list-like","open":"<span class='clj-list'>(</span>","close":"<span class='clj-list'>)</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-double'>6.026424987707287</span>","value":"6.026424987707287"},{"type":"html","content":"<span class='clj-double'>6.007193981204182</span>","value":"6.007193981204182"}],"value":"(6.026424987707287 6.007193981204182)"}],"value":"((-3.605326461372897 8.426766698248684) (0.7944855261594057 9.90083156619221) (-0.3857196127064526 5.327107526827604) (0.6304292923305184 3.839924451895058) (6.398137726588175 5.857059662230313) (-0.45716292154975235 1.0282586980611086) (-3.726910886587575 7.831128865946084) (6.026424987707287 6.007193981204182))"}],"value":"(0 ((-3.605326461372897 8.426766698248684) (0.7944855261594057 9.90083156619221) (-0.3857196127064526 5.327107526827604) (0.6304292923305184 3.839924451895058) (6.398137726588175 5.857059662230313) (-0.45716292154975235 1.0282586980611086) (-3.726910886587575 7.831128865946084) (6.026424987707287 6.007193981204182)))"}
;; <=

;; @@

;; @@

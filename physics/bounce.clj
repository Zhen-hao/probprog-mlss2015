;; Probabilistic programming inference
;; through a 2D physics engine; using
;; inference to do mechanism design, namely
;; positioning bumpers to safely deliver
;; a number of bouncy balls into a waiting
;; bin.
;;
;; Task: run the code below to see the "world"
;; in simulation.  Modify the query to
;; infer bumper locations that get
;; ideally all of the ten dropped balls into
;; the bin
;;
;; Frank Wood fwood@robots.ox.ac.uk
;; Brooks Paige brx@robots.ox.ac.uk

(ns bounce
  (:gen-class)
  (:require [org.nfrac.cljbox2d.testbed :as bed]
            [org.nfrac.cljbox2d.core :refer :all]
            [org.nfrac.cljbox2d.vec2d :as vec2d]
            [quil.core :as quil]
            [quil.middleware]
            )
  (:use [clojure.data.priority-map]
        [anglican [core :exclude [-main]]
           runtime emit [inference :only [infer]]
           [state :only [get-predicts get-log-weight set-log-weight]]]
        [anglib crp xrp]))


(def circ-attr {:density 1.0
                :category-bits 0x0008
                :mask-bits 0xFFFF})

(defn create-world
  "creates a world given bumper positions and ball bodies,
  which are used to copy bodies for post-inference display"
  [bumper-positions balls]
  (let [world (new-world)

        ; creates target cup
        cup_center 15
        cup_floor (body! world {:type :static}
                      {:shape (edge [(- cup_center 1) 0] [(+ cup_center 1) 0])
                       :friction 0.3 :restitution 0.0})
        cup_left_wall (body! world {:type :static}
                      {:shape (edge [(- cup_center 1) 0] [(- cup_center 1) 2])
                       :friction 0.3 :restitution 0.0})
        cup_right_wall (body! world {:type :static}
                      {:shape (edge [(+ cup_center 1) 0] [(+ cup_center 1) 2])
                       :friction 0.3 :restitution 0.0})

        ; creates ramp
        ramp (body! world {:type :static}
                      {:shape (edge [-6 9] [-4 7])
                       :friction 0.2 :restitution 0.0})

        ; if a list of balls are passed in re-create
        ; new balls at the old balls' positions
        _ (if (not (nil? balls))
            (loop [bs balls]
              (if (empty? bs)
                nil
                (let [b (first bs)]
                  (body! world {:position (position b)}
                     ; TODO generalize to copy body (or attrs),
                     ; not just create a new ball at the same
                     ; location  
                     (assoc circ-attr
                     :shape (circle 0.25) :restitution 0.5
                     :group-index 1))
                  (recur (rest bs))))))



        ; creates bumpers
        _ (loop [x bumper-positions]
            (if (empty? x)
              nil
              (let
                [[xx yy] (v2xy (first x))
                 ;_ (prn [xx yy])
                 lpos [(- xx 0.5) yy]
                 ;_ (prn lpos)
                 rpos [(+ xx 0.5) yy]
                 ;_ (prn rpos)
                 ]
                (body! world {:type :static}
                       {:shape (edge lpos rpos)
                       :friction 0.3 :restitution 0.9})
                (recur (rest x)))))]
    world))


(defn setup [bumper-positions balls]
  "creates world and sets up state for running quil simulation"
  (quil/frame-rate 30)
  (let [world (create-world bumper-positions balls)]
    (assoc bed/initial-state
      :last 0.0
      :numballs 0
      :world world)))

(defn step
  "steps the simulation one timestep forward (in quil simluation)
   creating 20 balls/bodies as the simulation runs forward"
  [state]
  (let [now (:time state)
        contacts (all-current-contacts (:world state))]
    (do
      (if (and (> now (+ (:last state)
                         (/ 30.0 30.0)))
               (< (:numballs state) 20))
        (do
          (body! (:world state)
                 {:position [-5 (+ 8.25 0.1)]};(sample (gamma 0.2 0.2)))]}
                 (assoc circ-attr
                   :shape (circle 0.25)
                   :restitution 0.5
                   :group-index 1))
          (-> (update-in state [:last] (fn [a b] b) now)
              (update-in [:numballs] + 1)
              (bed/world-step)
              (bed/record-snapshot true)))
        (-> (bed/world-step state)
            (bed/record-snapshot true))))))

(defn draw
  "quil state drawing function"
  [state]
  (bed/draw state)
  (quil/fill 255)
  (quil/text (str "Bounce.\n")
             10 10))

(defn simulate-world
  "simulates the world for 20 sec. outside of quil
  creating 10 balls, one every 2/3's of a sec."
  [world]
  (let [dt (/ 1.0 30.0)
        duration 20.0
        dts-between-balls 20
        bodies (loop [t 0.0 c 0 bodies []]
                       (if (> t duration)
                         bodies
                         (do
                           (step! world dt)
                           (if (and (= c dts-between-balls)
                                    (< (count bodies) 10))
                             (let [new-ball
                                   (body! world
                                     {:position
                                      [-5 (+ 8.25 0.1)]};(sample (gamma 0.2 0.2)))]}
                                     (assoc circ-attr
                                       :shape (circle 0.25)
                                       :restitution 0.5
                                       :group-index 1))]
                               (recur (+ t dt)
                                      0
                                      (conj bodies new-ball)))
                             (recur (+ t dt)
                                    (+ c 1)
                                    bodies)))))]
    {:balls bodies}))


(defn display-static-world [bumper-positions balls]
  "displays a static world with bumpers and balls"
 (quil/sketch
   :title "Bounce"
   :setup (partial setup bumper-positions balls)
   :update identity
   :draw draw
   :key-typed bed/key-press
   :mouse-pressed bed/mouse-pressed
   :mouse-released bed/mouse-released
   :mouse-dragged bed/mouse-dragged
   :mouse-wheel bed/mouse-wheel
   :size [600 500]
   :features [:resizable :keep-on-top]
   :middleware [quil.middleware/fun-mode]))

(defn show-world-simulation [bumper-positions]
  "simulates and displays a world with bumpers
  creating 20 balls during the simulation"
   (quil/sketch
   :title "Bounce"
   :setup (partial setup bumper-positions nil)
   :update (fn [s] (if (:paused? s) s (step s)))
   :draw draw
   :key-typed bed/key-press
   :mouse-pressed bed/mouse-pressed
   :mouse-released bed/mouse-released
   :mouse-dragged bed/mouse-dragged
   :mouse-wheel bed/mouse-wheel
   :size [600 500]
   :features [:resizable :keep-on-top]
   :middleware [quil.middleware/fun-mode]))

(defn linspace
  "creates a list of n values
  linearly distributed between min
  and max inclusive"
  [min max n]
   (let [step (/ (- max min) (dec n))]
      (range min (+ max step) step)))



;; The task: a Rube-Goldberg device design task of sorts;
;; given a ball emitter and a bin into which you would like
;; the balls to end up, use inference to configure
;; a set of bumpers such that the balls do end up in the bin

;; here's a guess as to where bumpers might go; the values used
;; should also give you a sense of the dimensions of the
;; world (fyi, the bin is at [15 0], is 2 units wide and has walls of height
;; 2, balls are dropped from around [-5 8.25] down a ramp that goes
;; from [-6 9] [-4 7])
(def bumper-location-guess (map vec2 (linspace -3 13 4) (linspace 6 2 4)))
;; create a world with given bumpers
(def guess-world (create-world bumper-location-guess nil))
;; simulate the world forward using a 2d physics engine
(def guess-world-final-state (simulate-world guess-world))
;; show the conclusion of said simulation -- note
;; that you see no balls which suggests they have missed
;; the target

(first (:balls guess-world-final-state))

(display-static-world bumper-location-guess
                       (:balls guess-world-final-state))
;; and to see why you can run the simulator in "real time"
;; watching it as it goes
(show-world-simulation bumper-location-guess)

;; instead of painstakingly hand tuning the bumper
;; locations; let's use ABC and inference to figure
;; out where to place them

;; we need an observable signal and the number
;; of balls in the box after 20 sec. of simulation
;; is something we can count; here's a function
;; to do that
(defn balls-in-box [world]
  "computes number of balls that
  are in the box in the given
  world";
  ;(-ish, problem specific hack)
  (->> (:balls world)
     (map position)
     (map second)
     (filter #(and (< 0 %) (< % 3)))
     (count)))

;; just to check, our guess at the bumper locations
;; leaves none in the box
(balls-in-box guess-world-final-state)


;; With these tools it should be reasonably
;; straightforward to write a query that
;; learns a distribution over configurations
;; that place a large number of balls
;; into the bin

;; to do this we've provided scaffolding
;; that can be modified to achieve
;; your objective
(with-primitive-procedures
  [vec2 create-world simulate-world balls-in-box position]
  (defquery arrange-bumpers []
    (let [;; -- change this --
          bumper-positions (list (vec2 8 6))

          ;; -- insert code here... --

          world (create-world bumper-positions nil)
          end-world (simulate-world world)
          balls (:balls end-world)

          obs-dist (normal 10 1)
          num-balls-in-box (balls-in-box end-world)]
      (observe obs-dist num-balls-in-box)
      (predict :balls balls)
      (predict :num-balls-in-box num-balls-in-box)
      (predict :bumper-positions bumper-positions))))

;; here we'll use the doquery syntax because
;; we'd like to monitor convergence; we'll
;; use lightweight metropolis hastings to
;; perform the query
(def sampler (doquery :lmh arrange-bumpers nil))

;; we will use a silly metroc for what is "best," namely
;; the configuration learned after 1500 sweeps, i.e.
;; single variable proposed changes
(def best-configuration (get-predicts (first (drop 1500 sampler))))

;; we can look at the best configuration
best-configuration

;; and look at convergence with respect to the
;; number of balls left in the box
(->> (take 1500 sampler)
     (map get-predicts)
     (map :num-balls-in-box))


;; this is how th eworld ended up in teh "best configuration"
(display-static-world (:bumper-positions best-configuration)
                       (:balls best-configuration))

;; and this is what it looks like when run with 20 balls
(show-world-simulation (:bumper-positions best-configuration))


;; good luck!

;; bonus : refactor the code to include observed "heuristic factors"
;; during simulation that, for instance, ensure that every
;; bumper gets touched or, again for instance, ensure that
;; no balls ever bounce to the left

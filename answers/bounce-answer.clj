;; one possible bounce answer

(with-primitive-procedures
  [create-world simulate-world balls-in-box]
  (defquery arrange-bumpers []
    (let [number-of-bumpers (sample (uniform-discrete 1 10))
          bumpydist (uniform-continuous 0 10)
          bumpxdist (uniform-continuous -5 14)
          bumper-positions (repeatedly
                            number-of-bumpers
                            #(vector (sample bumpxdist) (sample bumpydist)))
          world (create-world bumper-positions)
          end-world (simulate-world world)
          balls (:balls end-world)
          obs-dist (normal 10 1)
          num-balls-in-box (balls-in-box end-world)]
      (observe obs-dist num-balls-in-box)
      (predict :balls balls)
      (predict :num-balls-in-box num-balls-in-box)
      (predict :bumper-positions bumper-positions))))

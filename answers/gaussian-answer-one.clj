;;; Possible answers for Gaussian exercise, part one

(defquery gaussian-model [data]
  (let [mu (sample (normal 1 (sqrt 5)))
        sigma (sqrt 2)]
    (map (fn [x] (observe (normal mu sigma) x)) data)
    (predict :mu mu)))



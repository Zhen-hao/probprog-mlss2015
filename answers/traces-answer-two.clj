;;; Possible answers for poisson sampler exercise, part two

(defquery tilted-poisson [lambda]
  (let [L (exp (- lambda))]
    (loop [k 0
           p 1
           trace []]
      (let [k (inc k)
            u (sample (uniform-continuous 0 1))
            p (* p u)
            trace (conj trace u)
            large? (> (dec k) 3)]
        (if (<= p L)
          (do
            (observe (flip 0.9) large?)
            (predict :large? large?)
            (predict :k (dec k))
            (predict :trace trace))
          (recur k p trace))))))

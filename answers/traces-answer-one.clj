;;; Possible answers for poisson sampler exercise, part one

(defquery poisson-trace [lambda]
  (let [L (exp (- lambda))]
    (loop [k 0
           p 1
           trace []]
      (let [k (inc k)
            u (sample (uniform-continuous 0 1))
            p (* p u)
            trace (conj trace u)]
        (if (<= p L)
          (do
            (predict :large? (> (dec k) 3))
            (predict :k (dec k))
            (predict :trace trace))
          (recur k p trace))))))


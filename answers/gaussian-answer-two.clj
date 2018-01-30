;;; Possible answers for Gaussian exercise, part two

(defquery scientists [measurements]
  (let [mu (sample (normal 0 50))
        noise-levels (repeatedly
                       (count measurements)
                       #(sample (uniform-continuous 0 25)))]
    (map (fn [measurement noise-level] 
           (observe (normal mu noise-level) measurement))
         measurements noise-levels)
    (predict :x mu)
    (predict :noise noise-levels)))

(defquery scientists [measurements]
  (let [x (sample (normal 0 50))]
    (loop [data measurements
           stds []]
      (if (not (empty? data))
        (let [f (first data)]
          (let [s (sample (uniform-continuous 0 25))]
            (observe (normal x s) f)
            (recur (rest data) (conj stds s))))
        (do
          (predict :x x)
          (predict :noise stds))))))

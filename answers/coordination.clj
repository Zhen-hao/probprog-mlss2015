;;; Possible answers for coordination game exercise

;; With false belief:

(with-primitive-procedures [location]
  (defm amy-true-model [depth]
    (let [amy-location (location 0.6)
          not-bob-location (if (= (bob-confused (dec depth)) :pub)
                             :starbucks
                             :pub)]
      (observe amy-location not-bob-location)
      not-bob-location))

  (defm amy-false-model [depth]
    (let [amy-location (location 0.6)
          bob-location (bob-confused (dec depth))]
      (observe amy-location bob-location)
      bob-location))
  
  (defm bob-confused [depth]
    (let [bob-location (location 0.6)]
      (if (> depth 0)
        (let [amy-location (amy-false-model depth)]
          (observe bob-location amy-location)
          amy-location)
        (sample bob-location)))))

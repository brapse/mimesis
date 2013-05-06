(ns mimesis.core-test
  (:use clojure.test
        mimesis.core))

(deftest test-averager
  (testing "That it calculates the average"
    (let [averager (generate-averager)]
      (do 
        (averager 2)
        (averager 4)
        (is (= 4 (averager 6)))))))

(defn ratio [choices]
  (/ (count (filter identity choices))
     (count choices)))


(defn confidence-interval [trials alpha]
  ; Hoeffding confidense interval for bernoulli random variable
  (let [err (Math/sqrt (* (/ 1
                            (* 2 trials))
                          (Math/log (/ 2
                                       alpha))))]
    (list (- err) err)))

(deftest test-sampler
  (testing "Can sample %10 within an effective sampling rate within %95 confidense bounds"
    (let [sample-size 10
          line-length 10
          file-length 1000
          trials 100000
          sampler? (generate-sampler sample-size file-length)
          sample-rate (/ sample-size
                         (/ file-length
                            line-length))
          effective-sample-rate (ratio (take trials
                                             (repeatedly #(sampler? line-length))))
          [lower upper] (map (partial + sample-rate)
                      (confidence-interval trials 0.5))]
      (is (and (>= upper effective-sample-rate)
               (<= lower effective-sample-rate))))))

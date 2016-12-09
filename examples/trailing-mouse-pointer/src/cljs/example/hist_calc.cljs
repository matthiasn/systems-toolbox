(ns example.hist-calc)

(defn mean
  "From: https://github.com/clojure-cookbook/"
  [coll]
  (let [sum (apply + coll)
        count (count coll)]
    (if (pos? count)
      (/ sum count)
      0)))

(defn median
  "Modified from: https://github.com/clojure-cookbook/
   Adapted to return nil when collection empty."
  [sorted]
  (let [cnt (count sorted)
        halfway (quot cnt 2)]
    (if (empty? sorted)
      nil
      (if (odd? cnt)
        (nth sorted halfway)
        (let [bottom (dec halfway)
              bottom-val (nth sorted bottom)
              top-val (nth sorted halfway)]
          (mean [bottom-val top-val]))))))

(defn interquartile-range
  "Determines the interquartile range of values in a sequence of numbers.
   Returns nil when sequence empty or only contains a single entry."
  [sorted]
  (let [cnt (count sorted)
        half-cnt (quot cnt 2)
        q1 (median (take half-cnt sorted))
        q3 (median (take-last half-cnt sorted))]
    (when (and q3 q1) (- q3 q1))))

(defn percentile-range
  "Returns only the values within the given percentile range."
  [sorted percentile]
  (let [cnt (count sorted)
        keep-n (Math/ceil (* cnt (/ percentile 100)))]
    (take keep-n sorted)))

(defn freedman-diaconis-rule
  "Implements approximation of the Freedman-Diaconis rule for determing bin size
   in histograms: bin size = 2 IQR(x) n^-1/3 where IQR(x) is the interquartile
   range of the data and n is the number of observations in sample x. Argument
   is expected to be a sequence of numbers."
  [sample]
  (let [n (count sample)]
    (when (pos? n)
      (* 2 (interquartile-range sample) (Math/pow n (/ -1 3))))))

(defn round-up [n increment] (* (Math/ceil (/ n increment)) increment))
(defn round-down [n increment] (* (Math/floor (/ n increment)) increment))

(defn best-increment-fn
  "Takes a seq of increments, a desired number of intervals in histogram axis,
   and the range of the values in the histogram. Sorts the values in increments
   by dividing the range by each to determine number of intervals with this
   value, subtracting the desired number of intervals, and then returning the
   increment with the smallest delta."
  [increments desired-n rng]
  (first (sort-by #(Math/abs (- (/ rng %) desired-n)) increments)))

(defn default-increment-fn
  "Determines the increment between intervals in histogram axis.
   Defaults to increments in a range between 1 and 5,000,000."
  [rng]
  (if rng
    (let [multipliers (mapv #(Math/pow 10 %) (range 0 6))
          increments (flatten (mapv (fn [i] (mapv #(* i %) multipliers)) [1 2.5 5]))
          best-increment (best-increment-fn increments 5 rng)]
      (if (zero? (mod best-increment 1))
        (int best-increment)
        best-increment))
    1))

(defn histogram-calc
  "Calculations for histogram."
  [{:keys [data bin-cf max-bins increment-fn]}]
  (let [mx (apply max data)
        mn (apply min data)
        rng (- mx mn)
        increment-fn (or increment-fn default-increment-fn)
        increment (increment-fn rng)
        bin-size (max (/ rng max-bins) (* (freedman-diaconis-rule data) bin-cf))
        binned-freq (frequencies
                      (mapv (fn [n] (Math/floor (/ (- n mn) bin-size))) data))]
    {:mn             mn
     :mn2            (round-down (or mn 0) increment)
     :mx2            (round-up (or mx 10) increment)
     :rng            rng
     :increment      increment
     :binned-freq    binned-freq
     :binned-freq-mx (apply max (mapv (fn [[_ f]] f) binned-freq))
     :bins           (inc (apply max (mapv (fn [[v _]] v) binned-freq)))}))

(ns matthiasn.systems-toolbox.ui.histogram)

(def text-default {:stroke "none" :fill "black" :style {:font-size 12}})
(def text-bold (merge text-default {:style {:font-weight :bold :font-size 12}}))
(def x-axis-label (merge text-default {:text-anchor :middle}))
(def path-defaults {:fill :black :stroke :black :stroke-width 1})

(defn interquartile-range
  "Determines the interquartile range of values in a collection of numbers."
  [sample]
  (let [sorted (sort sample)
        n (count sorted)
        q1 (nth sorted (Math/floor (/ n 4)))
        q3 (nth sorted (Math/floor (* (/ n 4) 3)))
        iqr (- q3 q1)]
    iqr))

(defn percentile-range
  "Returns only the values within the given percentile range."
  [sample percentile]
  (let [sorted (sort sample)
        n (count sorted)
        keep-n (Math/ceil (* n (/ percentile 100)))]
    (take keep-n sorted)))

(defn freedman-diaconis-rule
  "Implements approximation of Freedman–Diaconis rule for determing bin size in histograms: bin size = 2 IQR(x) n^-1/3
   where IQR(x) is the interquartile range of the data and n is the number of observations in the sample x.
   Argument coll is expected to be a collection of numbers."
  [sample]
  (let [n (count sample)]
    (when (pos? n)
      (* 2 (interquartile-range sample) (Math/pow n (/ -1 3))))))

(defn round-up [n increment] (* (Math/ceil (/ n increment)) increment))
(defn round-down [n increment] (* (Math/floor (/ n increment)) increment))

(defn histogram-y-axis
  "Draws y-axis of a chart."
  [x y h mx]
  (let [increment (cond (> mx 250) 100 (> mx 100) 50 (> mx 50) 20 (> mx 25) 10 :else 5)
        rng (range 0 (inc (round-up mx increment)) increment)
        scale (/ h (dec (count rng)))]
    [:g
     [:path (merge path-defaults {:d (str "M" x " " y "l 0 " (* h -1) " z")})]
     (for [n rng]
       ^{:key (str "yt" n)}
       [:path (merge path-defaults {:d (str "M" x " " (- y (* (/ n increment) scale)) "l -" 6 " 0")})])
     (for [n rng]
       ^{:key (str "yl" n)}
       [:text (merge text-default {:x (- x 10) :y (- y (* (/ n increment) scale) -4) :text-anchor :end}) n])]))

(defn histogram-x-axis
  "Draws x-axis for histrogram."
  [x y mn mx w scale increment]
  (let [rng (range mn (inc mx) increment)]
    [:g
     [:path (merge path-defaults {:d (str "M" x " " y "l" w " 0 z")})]
     (for [n rng] ^{:key (str "xt" n)}
                  [:path (merge path-defaults {:d (str "M" (+ x (* (- n mn) scale)) " " y "l 0 " 6)})])
     (for [n rng] ^{:key (str "xl" n)} [:text (merge x-axis-label {:x (+ x (* (- n mn) scale)) :y (+ y 20)}) n])]))

(defn histogram-view
  "Renders a histogram for roundtrip times."
  [rtt-times x y w h x-label color bin-cf max-bins]
    (let [mx (apply max rtt-times)
          mn (apply min rtt-times)
          rng (- mx mn)
          increment (cond (> rng 3000) 1000
                          (> rng 1500) 500
                          (> rng 900) 200
                          (> rng 400) 100
                          (> rng 200) 50
                          (> rng 90) 20
                          :else 10)
          mx2 (round-up (or mx 100) increment)
          mn2 (round-down (or mn 0) increment)
          rng2 (- mx2 mn2)
          x-scale (/ w rng2)
          bin-size (max (/ rng max-bins) (* (freedman-diaconis-rule rtt-times) bin-cf))
          binned-freq (frequencies (map (fn [n] (Math/floor (/ (- n mn) bin-size))) rtt-times))
          binned-freq-mx (apply max (map (fn [[_ f]] f) binned-freq))
          bins (inc (apply max (map (fn [[v _]] v) binned-freq)))
          bar-width (/ (* rng x-scale) bins)
          y-scale (/ (- h 20) binned-freq-mx)]
      [:g
       (if (> bins 4)
         (for [[v f] binned-freq]
           ^{:key (str "bf" x "-" y "-" v "-" f)}
           [:rect {:x      (+ x (* (- mn mn2) x-scale) (* v bar-width))
                   :y      (- y (* f y-scale))
                   :fill   color :stroke "black"
                   :width  bar-width
                   :height (* f y-scale)}])
         [:text {:x (+ x (/ w 2)) :y (- y 50) :stroke "none" :fill "#DDD" :text-anchor :middle
                 :style {:font-weight :bold :font-size 24}} "insufficient data"])
       (histogram-x-axis x (+ y 7) mn2 mx2 w x-scale increment)
       [:text (merge x-axis-label text-bold {:x (+ x (/ w 2)) :y (+ y 48) :text-anchor :middle}) x-label]
       [:text (let [x-coord (- x 45) y-coord (- y (/ h 3)) rotate (str "rotate(270 " x-coord " " y-coord ")")]
                (merge x-axis-label text-bold {:x x-coord :y y-coord :transform rotate})) "Frequencies"]
       (histogram-y-axis (- x 7) y h (or binned-freq-mx 10))]))
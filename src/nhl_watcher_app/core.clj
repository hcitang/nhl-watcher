(ns nhl-watcher-app.core
  (:gen-class))
(require '[clojure.data.json :as json])
(require '[clj-http.client :as client])
(require '[java-time :as time])
(require '[lanterna.screen :as s])

(def teams {"New Jersey Devils" "Devils", 
            "New York Islanders" "Islanders", 
            "New York Rangers" "Rangers", 
            "Philadelphia Flyers" "Flyers", 
            "Pittsburgh Penguins" "Penguins", 
            "Boston Bruins" "Bruins", 
            "Buffalo Sabres" "Sabres", 
            "Montréal Canadiens" "Canadiens", 
            "Ottawa Senators" "Senators", 
            "Toronto Maple Leafs" "Maple Leafs", 
            "Carolina Hurricanes" "Hurricanes", 
            "Florida Panthers" "Panthers", 
            "Tampa Bay Lightning" "Lightning", 
            "Washington Capitals" "Capitals", 
            "Chicago Blackhawks" "Blackhawks", 
            "Detroit Red Wings" "Red Wings", 
            "Nashville Predators" "Predators", 
            "St. Louis Blues" "Blues", 
            "Calgary Flames" "Flames", 
            "Colorado Avalanche" "Avalanche", 
            "Edmonton Oilers" "Oilers", 
            "Vancouver Canucks" "Canucks", 
            "Anaheim Ducks" "Ducks", 
            "Dallas Stars" "Stars", 
            "Los Angeles Kings" "Kings", 
            "San Jose Sharks" "Sharks", 
            "Columbus Blue Jackets" "Blue Jackets", 
            "Minnesota Wild" "Wild", 
            "Winnipeg Jets" "Jets", 
            "Arizona Coyotes" "Coyotes", 
            "Vegas Golden Knights" "Golden Knights"})

(def base-url "https://statsapi.web.nhl.com")
(def schedule-path "/api/v1/schedule")
(def event-path "/api/v1/game/")
(def event-path-post "/feed/live")


(defn in-progress?
  [game]
  (= "In Progress" (:status game)))

(defn game-over?
  [game]
  (= "Final" (:status game)))

(defn show-time-remaining
  [game]
  (let [period (:current-period-ordinal game)
        time-remaining (:current-period-time-remaining game)]
        (str period " (" time-remaining ")")))

(defn game-final-type
  "Parses the game and returns either 'F', 'F/OT', and 'F/SO'"
  [game]
  (cond
    (not (game-over? game)) ""
    (= (:current-period-ordinal game) "OT") "F/OT"
    (= (:current-period-ordinal game) "SO") "F/SO"
    :else "F"))

(defn get-game-progress
  [game]
  (cond
    (in-progress? game) (show-time-remaining game)
    (game-over? game) (game-final-type game)
    :else (:status game)))

(defn get-game-line
  [game]
  (cond 
    (in-progress? game) (str (:away-team game) " " (:away-team-score game) "\t" (:home-team game) " " (:home-team-score game) "\t" (show-time-remaining game))
    (game-over? game) (str (:away-team game) " " (:away-team-score game) "\t" (:home-team game) " " (:home-team-score game) "\t" (game-final-type game))
    :else (str (:away-team game) "\t" (:home-team game) "\t" (:status game))))

(defn game-from-raw
  "Converts the raw map from the feed (clojure map), and converts it to a flatter map"
  [raw-game-info]
  (zipmap
    [:away-team :away-team-score :home-team :home-team-score :status :live-feed-path :id :current-period-time-remaining :current-period-ordinal :periods]
    [(teams (get-in raw-game-info [:teams :away :team :name]))
      (get-in raw-game-info [:teams :away :score])
      (teams (get-in raw-game-info [:teams :home :team :name]))
      (get-in raw-game-info [:teams :home :score])
      (get-in raw-game-info [:status :detailedState])
      (get-in raw-game-info [:link])
      (get-in raw-game-info [:gamePk])
      (get-in raw-game-info [:linescore :currentPeriodTimeRemaining])
      (get-in raw-game-info [:linescore :currentPeriodOrdinal])
      (get-in raw-game-info [:linescore :periods])
    ]))

(defn games-from-raw
  "Converts a vector of raw game maps and convert them into a better internal representation"
  [raw-games]
  (map game-from-raw raw-games)) 

(defn games-for-date
  "Returns the list of games for a given date (clojure.java-time)"
  [date]
  (let [date-string (time/format "yyyy-MM-dd" date)
        url (str base-url schedule-path "?startDate=" date-string "&endDate=" date-string "&expand=schedule.linescore")
        raw-json-games (get-in (json/read-str (:body (client/get url)) :key-fn #(keyword %)) [:dates 0 :games])]
    ;(println url)
    (games-from-raw raw-json-games)))

(defn get-game-events
  "Returns a list of the game events for the given game object or game id"
  [game]
  (cond
    (number? game) ; it's a game-id
      (let [url (str base-url event-path game event-path-post)
            raw-feed (json/read-str (:body (client/get url)) :key-fn #(keyword %))]
            ;(println url)
        (get-in raw-feed [:liveData :plays :allPlays]))
    (contains? game :live-feed-path) ; it's a game object
      (let [url (str base-url (:live-feed-path game))
          raw-feed (json/read-str (:body (client/get url)) :key-fn #(keyword %))]
          ;(println url)
        (get-in raw-feed [:liveData :plays :allPlays]))))


(defn decorated-event-string
  [event]
  (let [result (:result event)]
    (cond
      (= "GOAL" (:eventTypeId result)) (str "🚨" (:event result) "🚨")
      (= "HIT" (:eventTypeId result)) (str "💥" (:event result) "💥")
      (= "PENALTY" (:eventTypeId result)) (str "👮" (:event result) "👮")
      :else (:event result))))

(defn game-event-string
  [event]
  (let [about (:about event)
        result (:result event)
        triCode (if (contains? event :team) (get-in event [:team :triCode]) "")
        decorated-event (decorated-event-string event)]
  (cond
    (= "GOAL" (:eventTypeId result)) (str (:ordinalNum about) " " (:periodTimeRemaining about) ": " triCode " " decorated-event ": " (:description result) " (" (get-in result [:strength :code]) ")")
    :else (str (:ordinalNum about) " " (:periodTimeRemaining about) ": " triCode " " decorated-event ": " (:description result)))))
; 👊 hit
; :team :triCode


(def header {:fg :white, :bg :blue})
(def highlight {:fg :black, :bg :white})

(defn clear-row
    [scr row options]
    (let [cols (first (s/get-size scr))
          spaces (apply str (repeat cols \space))]
        (s/put-string scr 0 row spaces options)))

(defn pprint-score-statline
    [game print-leading-spaces]
    (let [home (:home-team game)
          away (:away-team game)
          home-score (:home-team-score game)
          away-score (:away-team-score game)
          status (get-game-progress game)
          leading-spaces (- 26 (+ (count home) (count away)))]
          (if (true? print-leading-spaces)
            (str (apply str (repeat leading-spaces \space))
                away " @ " home " "
                away-score " - " home-score "  "
                status)
            (str away " @ " home " "
                away-score " - " home-score "  "
                status))))


(defn pprint-event
    [event]
    (game-event-string event))

(defn draw-score-screen
    [scr display-info]
    (let [highlight-row (:highlight-row display-info)
            date (:date display-info)
            games (:games display-info)]
            (s/put-string scr 0 0 (str "NHL Scores for " date) header)
            (loop [games games
                draw-row 1]
                (if (empty? games)
                        nil
                        (do
                            (if (= highlight-row draw-row)
                                (s/put-string scr 3 draw-row (pprint-score-statline (first games) true) highlight)
                                (s/put-string scr 3 draw-row (pprint-score-statline (first games) true)))
                            (recur
                                (rest games)
                                (inc draw-row)))))))

(defn draw-events-screen
    [scr display-info]
    (let [highlight-row (:highlight-row display-info)
            date (:date display-info)
            max-rows (- (second (s/get-size scr)) 2)
            max-event-index (:max-event-index display-info)
            detail-game (:detail-game display-info)
            game-events (:game-events display-info)]
        (s/put-string scr 0 0 (str date ": " (pprint-score-statline detail-game false)) header)
        (loop [game-events-to-show (take max-rows (reverse (filter #(>= max-event-index (get-in % [:about :eventIdx])) game-events)))
                draw-row 1]
                (if (empty? game-events-to-show)
                    nil
                    (do
                        (if (= highlight-row draw-row)
                            (s/put-string scr 3 draw-row (pprint-event (first game-events-to-show)) highlight)
                            (s/put-string scr 3 draw-row (pprint-event (first game-events-to-show))))
                        (recur
                            (rest game-events-to-show)
                            (inc draw-row)))))))

(defn draw-screen
    [scr display-info]
    (if (nil? display-info) 
        nil
        (let [highlight-row (:highlight-row display-info)
            time-to-update (:time-to-update display-info)
            detail-game (:detail-game display-info)]
            (s/clear scr)
            (clear-row scr 0 header)
            (clear-row scr highlight-row highlight)
            (s/put-string scr 0 (- (second (s/get-size scr)) 1) (str "Auto-update (*), last updated " (- 60 time-to-update) "s ago"))
            (if (nil? detail-game)
                (draw-score-screen scr display-info)
                (draw-events-screen scr display-info))
            (s/redraw scr)
            display-info)))

(defn update-time-to-update
    [display-info]
    (let [updated-time-to-update (dec (:time-to-update display-info))]
        (assoc display-info :time-to-update updated-time-to-update)))

(defn process-key-input-scores-view
    [key display-info]
    (let [date (:date display-info)
          games (:games display-info)
          highlight-row (:highlight-row display-info)
          time-to-update (:time-to-update display-info)
          detail-game (:detail-game display-info)
          max-event-index (:max-event-index display-info)]
        (cond ; in scores view
            (and (or (= key :up) (= key \k)) (> highlight-row 1)) 
                (assoc display-info :highlight-row (- highlight-row 1))
            (and (or (= key :down) (= key \j)) (< highlight-row (count games)))
                (assoc display-info :highlight-row (+ highlight-row 1))
            (or (= key :left) (= key \h))
                (assoc display-info :date (time/minus date (time/days 1)) :highlight-row 1
                    :games (games-for-date (time/minus date (time/days 1))))
            (or (= key :right) (= key \l))
                (assoc display-info :date (time/plus date (time/days 1)) :highlight-row 1
                    :games (games-for-date (time/plus date (time/days 1))))
            (or (= key \r) (< time-to-update 0))
                (assoc display-info :time-to-update 60
                    :games (games-for-date date))
            (= key \t)
                (assoc display-info :time-to-update 60 :date (time/local-date)
                    :games (games-for-date (time/local-date)))
            (= key \q)
                nil
            (= key :enter)
                (let [current-game (nth games (- highlight-row 1))
                    game-events (get-game-events current-game)
                    max-event-index (count game-events)]
                    ;(println "Current game: " (pprint-score-statline current-game false))
                    (assoc display-info
                        :time-to-update 60
                        :detail-game current-game 
                        :game-events game-events
                        :max-event-index max-event-index
                        :highlight-row 1))
            :else 
                display-info)))

(defn process-key-input-game-detail-view
    [key display-info]
    (let [date (:date display-info)
        games (:games display-info)
        highlight-row (:highlight-row display-info)
        time-to-update (:time-to-update display-info)
        detail-game (:detail-game display-info)
        max-event-index (:max-event-index display-info)]
        (cond ; in game event view
            (or (= key :escape) (= key \t) (= key \u))
                (assoc display-info :highlight-row 1 :detail-game nil) ; TODO: search through games to highlight the one I was looking at
            (or (= key :up) (= key \k))
                (if (> highlight-row 1) 
                    (assoc display-info :highlight-row (- highlight-row 1))
                    (assoc display-info :max-event-index (inc max-event-index))) ; TODO: check that we aren't going over what is possible in terms of events
            (or (= key :down) (= key \j))
                (let [max-rows (:max-rows display-info)]
                    (if (< highlight-row max-rows)
                        (assoc display-info :highlight-row (+ highlight-row 1)) ; TODO: make sure we don't go beyond what number of event lines we have
                        (assoc display-info :max-event-index (dec max-event-index))))
            (or (= key \r) (< time-to-update 0))
                (let [game-events (get-game-events detail-game)
                    max-event-index (count game-events)
                    games (games-for-date date)]
                    (assoc display-info :game-events game-events :max-event-index max-event-index :time-to-update 60 :highlight-row 1 :games (games-for-date date)))
            (= key \q)
                nil
            :else 
                display-info)))

        ; (or (= key :left) (= key \h))
        ;     (assoc display-info 
        ;         :time-to-update time-to-update 
        ;         :date (time/minus date (time/days 1))
        ;         :games (games-for-date (time/minus date (time/days 1))))
        ; (or (= key :right) (= key \l))
        ;     (assoc display-info 
        ;         :time-to-update time-to-update 1 
        ;         :date (time/plus date (time/days 1))
        ;         :games (games-for-date (time/plus date (time/days 1))))

(defn process-key-input
    [key display-info]
    (let [detail-game (:detail-game display-info)]
        (update-time-to-update
            (if (nil? detail-game)
                (process-key-input-scores-view key display-info)
                (process-key-input-game-detail-view key display-info)))))

(defn main-loop
    [scr display-info]
    (recur 
        scr
        (draw-screen 
            scr 
            (process-key-input 
                (s/get-key-blocking scr {:timeout (* 1000 (:time-to-update display-info))}) 
                (assoc display-info :max-rows (- (second (s/get-size scr)) 5))))))

(defn -main
"I don't do a whole lot ... yet."
    [& args]
    ;(println "Hello, World!")
    (let [scr (s/get-screen :text)
        date (time/minus (time/local-date) (time/days 1))
        games (games-for-date date)]
        (s/in-screen scr
            (s/move-cursor scr (map #(- % 1) (s/get-size scr)))
            (main-loop 
                scr
                {:highlight-row 1, :time-to-update 0, :date date, :games games, :detail-game nil}))))
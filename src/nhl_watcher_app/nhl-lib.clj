(ns nhl-lib.core
    (:gen-class)
    (:require [clojure.data.json :as json])
    (:require [clj-http.client :as client])
    (:require [clj-time.core :as t])
    (:require [clj-time.format :as f])
    (:require [clj-time.local :as l])
    (:use clojure.pprint))

    (def team-clubname {"New Jersey Devils" "Devils", 
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
(def schedule-path (str base-url "/api/v1/schedule"))
(def events-url #(str base-url %))

(def custom-formatter (f/formatter "yyyy-MM-dd"))
(def custom-time-formatter (f/formatter "HH:mm"))

(defn games-for-date
    "Returns the list of games for a given date (clojure.java-time)"
    [date]
    (let [date-string (f/unparse custom-formatter date)
          url (str base-url schedule-path "?startDate=" date-string "&endDate=" date-string "&expand=schedule.linescore")
          raw-json (json/read-str (:body (client/get url)) :key-fn #(keyword %))]
          (:games (first (:dates raw)))))

(defn away-team
    [game]
    (-> game :teams :away :team :name))

(defn home-team
    [game]
    (-> game :teams :home :team :name))

(defn home-team-goals
    [game]
    (-> game :teams :home :score))

(defn away-team-goals
    [game]
    (-> game :teams :away :score))

(defn game-status
    [game]
    ; or
    ; (format "%s (%s)" (:currentPeriodOrdinal game) (:currentPeriodTimeRemaining game))
    (-> game :status :detailedState))

(defn game-periods
    [game]
    (-> game :linescore :periods))

(defn per-period
    [thing game]
    (let [periods (game-periods game)
          period-count (count periods)
          teams [:away :home]
          thing-fn #(thing (%1 %2))
          away-things (into [] (map thing-fn (repeat period-count :away) periods))
          home-things (into [] (map thing-fn (repeat period-count :home) periods))]
        (zipmap teams [away-things home-things])))

(defn sog-per-period
    [game]
    (per-period :shotsOnGoal game))

(defn goals-per-period
    [game]
    (per-period :goals game))

(def yesterday (t/minus (t/now) (t/days 1)))
(def today (t/now))

(defn get-statline
    [game]
    (let [team-line (format "%s @ %s" (team-clubname (away-team game)) (team-clubname (home-team game)))
          score-line (format "%s - %s" (away-team-goals game) (home-team-goals game))
          status-line (game-status game)]
        {:team-line team-line, :score-line score-line, :status-line status-line}))

(defn pprint-score-statline
    [game print-leading-spaces]
    (let [{:keys [:team-line :score-line :status-line]} (get-statline game)]
        )

(defn pprint-score-statline
    [game print-leading-spaces]
    (let [{:keys [:home-team :away-team :home-team-score :away-team-score]} game
          teams (str away-team " @ " home-team)
          score (str away-team-score " - " home-team-score)
          status (get-game-progress game)
          leading-space-count (- 26 (+ (count home-team) (count away-team)))
          leading-spaces (if (true? print-leading-spaces)
                            (str (apply str (repeat leading-space-count \space)))
                            "")]
        (if (scheduled? game) 
            (str leading-spaces teams "  " status)
            (str leading-spaces teams " " score "  " status))))





(defn get-game-events
    "Returns a list of the game events for the given game object or game id"
    [game]
    (cond
        (number? game) ; it's a game-id
        (let [url (str base-url event-path game event-path-post)
                raw-feed (json/read-str (:body (client/get url)) :key-fn #(keyword %))]
            (get-in raw-feed [:liveData :plays :allPlays]))
        (contains? game :live-feed-path) ; it's a game object
        (let [url (str base-url (:live-feed-path game))
                raw-feed (json/read-str (:body (client/get url)) :key-fn #(keyword %))]
            (get-in raw-feed [:liveData :plays :allPlays]))))



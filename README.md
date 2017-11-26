# nhl-watcher
"Watch" NHL game events live at a text terminal, and view scores

Through a text-based interface, shows up-to-date scores of games, and up-to-the-minute events in live games.

This a first project made with Clojure. It is not really meant for public consumption. Maybe someone can help me do a code review to teach me how to structure the code better.

Uses the undocumented NHL API. (It turns out there's a [way better library](https://github.com/peruukki/nhl-score-api), and other projects should probably use that instead.)

View the action here (unfortunately, not live):
https://asciinema.org/a/SE9F2uU9VjmfCPleVJY6SoNpb

Run with

    lein run {options}

        options:
          :text - render in terminal
          :swing - render in a swing window

Keys: 
* up, down, left, right, and enter to navigate the scores screen
* r to refresh
* t to go to today
* esc or u to back out of the game detail view


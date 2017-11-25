# nhl-watcher
"Watch" NHL game events live at a text terminal, and view scores

This a project with Clojure. Not really meant for public consumption. Hopefully, someone can look at this and learn what they *shouldn't* do with code. ;-)

Uses the undocumented NHL API (found out later that there's a [way better library](https://github.com/peruukki/nhl-score-api) that I should have used to consume it)

View the action here (unfortunately, not live):
https://asciinema.org/a/Fv7PuqD4nCCxVc8iRaeBBvixO

Run with

  lein run

Keys: 
* up, down, left, right, and enter to navigate the scores screen
* r to refresh
* t to go to today
* esc to back out of the game detail view


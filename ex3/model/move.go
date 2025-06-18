package model

type Move struct {
	PlayerName string
	Move       string
	ResultChan chan string
}

var Moves = []string{"rock", "paper", "scissors"}

var Beats = map[string]string{
	"rock":     "scissors",
	"scissors": "paper",
	"paper":    "rock",
}

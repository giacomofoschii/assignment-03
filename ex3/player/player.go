package player

import (
	"fmt"
	"math/rand"

	"ex3/model"
)

func Player(name string, requestChan <-chan chan model.Move) {
	score := 0
	for {
		resultChan := make(chan string)
		move := model.Moves[rand.Intn(len(model.Moves))]

		moveStruct := model.Move{
			PlayerName: name,
			Move:       move,
			ResultChan: resultChan,
		}

		responseChan := <-requestChan
		responseChan <- moveStruct

		result := <-resultChan
		if result == "win" {
			score++
		}

		fmt.Printf("[%s] played %s | Result: %s | Score: %d\n", name, move, result, score)
	}
}

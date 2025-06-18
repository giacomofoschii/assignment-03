package referee

import (
	"ex3/model"
	"fmt"
	"time"
)

func Referee(p1Chan, p2Chan chan chan model.Move) {
	for round := 1; ; round++ {
		fmt.Printf("\n--- Round %d ---\n", round)

		moveChan1 := make(chan model.Move)
		moveChan2 := make(chan model.Move)

		p1Chan <- moveChan1
		p2Chan <- moveChan2

		move1 := <-moveChan1
		move2 := <-moveChan2

		result1, result2 := "draw", "draw"
		if move1.Move != move2.Move {
			if model.Beats[move1.Move] == move2.Move {
				result1 = "win"
				result2 = "lose"
			} else {
				result1 = "lose"
				result2 = "win"
			}
		}

		move1.ResultChan <- result1
		move2.ResultChan <- result2

		// 👇 Rallenta per leggere gli output
		time.Sleep(1 * time.Second)
	}
}

import { applyReview, getState, setState } from '@/utils/sm2'

describe('SM-2 simplified algorithm', () => {
  const id = 'test-word'

  beforeEach(() => {
    localStorage.clear()
  })

  it('initial correct answer increases repetitions and sets interval', () => {
    const state = applyReview(id, true)
    expect(state.repetitions).toBe(1)
    expect(state.interval).toBe(1)
    expect(getState(id)).toBeTruthy()
  })

  it('second correct sets interval to 6', () => {
    setState(id, { repetitions: 1, easeFactor: 2.5, interval: 1 })
    const state = applyReview(id, true)
    expect(state.interval).toBe(6)
    expect(state.repetitions).toBe(2)
  })

  it('wrong answer resets repetitions', () => {
    setState(id, { repetitions: 3, easeFactor: 2.5, interval: 10 })
    const state = applyReview(id, false)
    expect(state.repetitions).toBe(0)
    expect(state.interval).toBe(1)
  })
})



"""Count-down latch matching java.util.concurrent.CountDownLatch semantics."""

from __future__ import annotations

import threading


class CountDownLatch:
    def __init__(self, count: int) -> None:
        if count < 0:
            raise ValueError("count must be non-negative")
        self._count = count
        self._condition = threading.Condition()

    def count_down(self) -> None:
        with self._condition:
            self._count -= 1
            if self._count <= 0:
                self._condition.notify_all()

    def await_zero(self) -> None:
        with self._condition:
            while self._count > 0:
                self._condition.wait()

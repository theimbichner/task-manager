import time

class DateTime:
    def __init__(self, start=None, end=None):
        if start is None:
            start = int(time.time())
        if end is None:
            end = start
        self.__start = start
        self.__end = end

    def __eq__(self, other):
        if isinstance(other, DateTime):
            return self.start == other.start and self.end == other.end
        return False

    @property
    def start(self):
        return self.__start

    @property
    def end(self):
        return self.__end

    def to_dict(self):
        return {
            'start': self.__start,
            'end': self.__end
        }

    @staticmethod
    def from_dict(data):
        return DateTime(data['start'], data['end'])

    def with_duration(self, duration):
        return DateTime(self.__start, self.__start + duration)

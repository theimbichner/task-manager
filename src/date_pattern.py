# get_dates must be inclusive of the end and exclusive of the start
class UnionDatePattern:
    def __init__(self, date_patterns):
        self.date_patterns = date_patterns

    def to_dict(self):
        return {
            'datePatterns': self.date_patterns
        }

    @staticmethod
    def from_dict(data):
        return UnionDatePattern(data['datePatterns'])

    def get_dates(self, start, end):
        return [date for p in self.date_patterns for date in p.get_dates(start, end)]

class UniformDatePattern:
    def __init__(self, initial_time, delta_time):
        self.initial_time = initial_time
        self.delta_time = delta_time

    def to_dict(self):
        return {
            'initialTime': self.initial_time,
            'deltaTime': self.delta_time
        }

    @staticmethod
    def from_dict(data):
        return UniformDatePattern(data['initialTime'], data['deltaTime'])

    def get_dates(self, start, end):
        if initial_time > start:
            time = initial_time
        else:
            n = (start - initial_time) // delta_time + 1
            time = initial_time + n * delta_time
        result = []
        while time <= end:
            result.append(time)
            time += delta_time
        return result

# TODO
class MonthlyDatePattern:
    def __init__(self):
        pass

    def to_dict(self):
        pass

    @staticmethod
    def from_dict(data):
        pass

    def get_dates(self, start, end):
        pass

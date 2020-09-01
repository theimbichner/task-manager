import time

DATA_TYPE_FIELD = '$dataType'

class DateTime:
    def __init__(self, start=None, end=None):
        if start is None:
            start = int(time.time())
        if end is None:
            end = start
        self.__start = start
        self.__end = end

    @property
    def start(self):
        return self.__start

    @property
    def end(self):
        return self.__end

    def to_dict(self):
        return {
            DATA_TYPE_FIELD: type(self).__name__,
            'start': self.__start,
            'end': self.__end
        }

    @staticmethod
    def from_dict(data):
        return DateTime(data['start'], data['end'])

    def with_duration(self, duration):
        return DateTime(self.__start, self.__start + duration)

# TODO validate type_name
# Number
# Integer
# String
# Boolean
# DateTime
# Enum
# EnumList

# Notion data types not implemented
# Person
# File
# Url
# Email
# Phone
# Relation/Rollup
# Creator
# Last Modifier

class TypeDescriptor:
    def __init__(self, type_name):
        self.type_name = type_name

    def to_dict(self):
        return {
            DATA_TYPE_FIELD: type(self).__name__,
            'typeName': self.type_name
        }

    @staticmethod
    def from_dict(data):
        return TypeDescriptor(data['typeName'])

    def new_default_value(self):
        return None

class EnumerationTypeDescriptor:
    def __init__(self, is_list, enum_values):
        self.is_list = is_list
        self.enum_values = enum_values

    def to_dict(self):
        return {
            DATA_TYPE_FIELD: type(self).__name__,
            'isList': self.is_list,
            'enumValues': self.enum_values
        }

    @staticmethod
    def from_dict(data):
        return EnumerationTypeDescriptor(data['isList'], data['enumValues'])

    def new_default_value(self):
        return [] if self.is_list else None

# get_dates must be inclusive of the end and exclusive of the start
class UnionDatePattern:
    def __init__(self, date_patterns):
        self.date_patterns = date_patterns

    def to_dict(self):
        return {
            DATA_TYPE_FIELD: type(self).__name__,
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
            DATA_TYPE_FIELD: type(self).__name__,
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

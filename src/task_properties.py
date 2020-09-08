from src.task_data import DATA_TYPE_FIELD

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

    # TODO set default values
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

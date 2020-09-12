# Notion data types not implemented
# Person
# File
# Url
# Email
# Phone
# Relation/Rollup
# Creator
# Last Modifier

def get_type(type_name):
    if type_name in {'Number', 'Integer', 'String', 'Boolean', 'DateTime'}:
        return TypeDescriptor(type_name)
    elif type_name in {'Enum', 'EnumList'}:
        return EnumerationTypeDescriptor(type_name == 'EnumList', [])
    else:
        raise ValueError

class TypeDescriptor:
    def __init__(self, type_name):
        self.type_name = type_name

    def to_dict(self):
        return {
            'typeName': self.type_name
        }

    @staticmethod
    def from_dict(data):
        return TypeDescriptor(data['typeName'])

    default_values = {
        'String': '',
        'Boolean': False
    }

    def default_value(self):
        return TypeDescriptor.default_values.get(self.type_name)

class EnumerationTypeDescriptor:
    def __init__(self, permit_multiple, enum_values):
        self.permit_multiple = permit_multiple
        self.enum_values = enum_values

    def to_dict(self):
        return {
            'permitMultiple': self.permit_multiple,
            'enumValues': self.enum_values
        }

    @staticmethod
    def from_dict(data):
        return EnumerationTypeDescriptor(data['permitMultiple'], data['enumValues'])

    def default_value(self):
        return () if self.permit_multiple else None

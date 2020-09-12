import pytest

import src.task_properties as task_properties

def test_to_from_dict():
    descriptor = task_properties.get_type('Integer')
    data = descriptor.to_dict()
    new_descriptor = task_properties.TypeDescriptor.from_dict(data)
    assert new_descriptor.type_name == descriptor.type_name

def test_enum_to_from_dict():
    descriptor = task_properties.EnumerationTypeDescriptor(False, ['a', 'b', 'c'])
    data = descriptor.to_dict()
    new_descriptor = task_properties.EnumerationTypeDescriptor.from_dict(data)
    assert new_descriptor.permit_multiple == descriptor.permit_multiple
    assert new_descriptor.enum_values == descriptor.enum_values

default_value_data = [
    ('Number', None),
    ('Integer', None),
    ('String', ''),
    ('Boolean', False),
    ('DateTime', None),
    ('Enum', None),
    ('EnumList', ())
]

@pytest.mark.parametrize('type_name, expected', default_value_data)
def test_default_value(type_name, expected):
    descriptor = task_properties.get_type(type_name)
    assert descriptor.default_value() == expected

def test_get_invalid_type():
    with pytest.raises(ValueError):
        task_properties.get_type('invalid')

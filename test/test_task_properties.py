import pytest

import src.task_properties as task_properties

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

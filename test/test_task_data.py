import json

import pytest

import src.date_pattern as date_pattern
import src.task_data as task_data
import src.task_properties as task_properties
import src.task_time as task_time

test_structure = [
    date_pattern.UniformDatePattern(11, 13),
    date_pattern.UnionDatePattern([
        date_pattern.UniformDatePattern(75, 7),
        date_pattern.UniformDatePattern(93, 1),
        date_pattern.UniformDatePattern(6, 24),
        date_pattern.UniformDatePattern(89, 4)
    ]),
    task_time.DateTime(123456, 456123),
    task_properties.get_type('Integer'),
    task_properties.EnumerationTypeDescriptor(False, ['a', 'b', 'c']),
    {'alpha': 'beta'}
]

def test_read_write_data_objects():
    json_string = json.dumps(test_structure, default=task_data.write_data_objects)
    result = json.loads(json_string, object_hook=task_data.read_data_objects)
    assert result[0].get_dates(0, 1000) == test_structure[0].get_dates(0, 1000)
    assert result[1].get_dates(0, 1000) == test_structure[1].get_dates(0, 1000)
    assert result[2] == test_structure[2]
    assert result[3].type_name == test_structure[3].type_name
    assert result[4].permit_multiple == test_structure[4].permit_multiple
    assert result[4].enum_values == test_structure[4].enum_values
    assert result[5] == test_structure[5]

def test_write_data_objects_invalid():
    with pytest.raises(TypeError):
        json.dumps(set(), default=task_data.write_data_objects)

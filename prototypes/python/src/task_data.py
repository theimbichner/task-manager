import src.date_pattern as date_pattern
import src.task_properties as task_properties
import src.task_time as task_time

DATA_TYPE_FIELD = '$dataType'

_DATA_CLASSES = (
    task_time.DateTime,
    task_properties.TypeDescriptor,
    task_properties.EnumerationTypeDescriptor,
    date_pattern.UnionDatePattern,
    date_pattern.UniformDatePattern,
    date_pattern.MonthlyDatePattern
)

def read_data_objects(data):
    data_type = data.get(DATA_TYPE_FIELD)
    classes = [x for x in _DATA_CLASSES if x.__name__ == data_type]
    if classes:
        return classes[0].from_dict(data)
    return data

def write_data_objects(obj):
    if any(isinstance(obj, x) for x in _DATA_CLASSES):
        data = obj.to_dict()
        data[DATA_TYPE_FIELD] = type(obj).__name__
        return data
    raise TypeError

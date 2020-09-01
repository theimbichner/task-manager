import task
import task_data

from functools import lru_cache

_TASKS_FOLDER_NAME = 'tasks'
_GENERATORS_FOLDER_NAME = 'generators'
_TABLES_FOLDER_NAME = 'tables'

_DATA_CLASSES = (
    task_data.DateTime,
    task_data.TypeDescriptor,
    task_data.EnumerationTypeDescriptor,
    task_data.UnionDatePattern,
    task_data.UniformDatePattern,
    task_data.MonthlyDatePattern
)

@lru_cache
def get_task_by_id(task_id):
    return _get_by_id(task_id, _TASKS_FOLDER_NAME, task.Task)

@lru_cache
def get_generator_by_id(generator_id):
    return _get_by_id(generator_id, _GENERATORS_FOLDER_NAME, task.Generator)

@lru_cache
def get_table_by_id(table_id):
    return _get_by_id(table_id, _TABLES_FOLDER_NAME, task.Table)

def persist_task(task):
    _persist(task, _TASKS_FOLDER_NAME)

def persist_generator(generator):
    _persist(generator, _GENERATORS_FOLDER_NAME)

def persist_table(table):
    _persist(table, _TABLES_FOLDER_NAME)

# TODO use some specified base directory instead of ./
# TODO what if the item doesn't exist
def _get_by_id(obj_id, folder, cls):
    path = f'./{folder}/{obj_id}.json'
    with open(path, 'r') as f:
        data = json.load(f, object_hook=_read_data_objects)
        return cls.from_dict(data)

def _persist(obj, folder):
    path = f'./{folder}/{obj.id}.json'
    with open(path, 'w') as f:
        data = obj.to_dict()
        json.dump(data, f, default=_write_data_objects)

def _read_data_objects(data):
    data_type = data.get(task_data.DATA_TYPE_FIELD)
    classes = [x for x in _DATA_CLASSES if x.__name__ == data_type]
    if classes:
        return classes[0].from_dict(data)
    return data

def _write_data_objects(obj):
    if any(isinstance(obj, x) for x in _DATA_CLASSES):
        return obj.to_dict()
    raise TypeError

import task
import task_data

from functools import lru_cache

TASKS_FOLDER_NAME = 'tasks'
GENERATORS_FOLDER_NAME = 'generators'
TABLES_FOLDER_NAME = 'tables'

DATA_CLASSES = (
    task_data.DateTime,
    task_data.TypeDescriptor,
    task_data.EnumerationTypeDescriptor,
    task_data.UnionDatePattern,
    task_data.UniformDatePattern,
    task_data.MonthlyDatePattern
)

@lru_cache
def get_task_by_id(task_id):
    return get_by_id(task_id, TASKS_FOLDER_NAME, task.Task)

@lru_cache
def get_generator_by_id(generator_id):
    return get_by_id(generator_id, GENERATORS_FOLDER_NAME, task.Generator)

@lru_cache
def get_table_by_id(table_id):
    return get_by_id(table_id, TABLES_FOLDER_NAME, task.Table)

def persist_task(task):
    persist(task, TASKS_FOLDER_NAME)

def persist_generator(generator):
    persist(generator, GENERATORS_FOLDER_NAME)

def persist_table(table):
    persist(table, TABLES_FOLDER_NAME)

# TODO use some specified base directory instead of ./
def get_by_id(obj_id, folder, cls):
    path = f'./{folder}/{obj_id}.json'
    with open(path, 'r') as f:
        data = json.load(f, object_hook=read_objects)
        return cls.from_dict(data)

def persist(obj, folder):
    path = f'./{folder}/{obj.id}.json'
    with open(path, 'w') as f:
        data = obj.to_dict()
        json.dump(data, f, default=write_objects)

def read_objects(data):
    data_type = data.get(task.DATE_TYPE_FIELD)
    classes = [x for x in DATA_CLASSES if x.__name__ == data_type][0]
    if classes:
        return classes[0].from_dict(data)
    return data

def write_objects(obj):
    if any(isinstance(obj, x) for x in DATA_CLASSES):
        return obj.to_dict()
    raise TypeError

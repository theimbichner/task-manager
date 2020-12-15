import src.task as task
import src.task_data as task_data

from functools import lru_cache

_TASKS_FOLDER_NAME = 'tasks'
_GENERATORS_FOLDER_NAME = 'generators'
_TABLES_FOLDER_NAME = 'tables'

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
        data = json.load(f, object_hook=task_data.read_data_objects)
        return cls.from_dict(data)

def _persist(obj, folder):
    path = f'./{folder}/{obj.id}.json'
    with open(path, 'w') as f:
        data = obj.to_dict()
        json.dump(data, f, default=task_data.write_data_objects)

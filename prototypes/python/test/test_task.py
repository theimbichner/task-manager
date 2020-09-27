import time

import src.task as task

def setup_module(module):
    module.schema = {}

def test_init_table():
    start = int(time.time())
    table = task.Table('Table Name', schema)
    end = int(time.time())

    assert start <= table.date_created.start
    assert end >= table.date_created.start

    assert table.date_created.start == table.date_created.end
    assert table.date_created == table.date_last_modified

    assert table.id # TODO
    assert table.name == 'Table Name'
    assert table.tasks == []
    assert table.generators == []
    assert table.schema == schema

def test_init_task():
    pass

def test_init_generator():
    pass

def test_task_to_dict():
    pass

def test_task_from_dict():
    pass

def test_modify_task():
    pass

def test_modify_following_tasks():
    pass

def test_generator_to_dict():
    pass

def test_generator_from_dict():
    pass

def test_modify_generator():
    pass

def test_table_to_dict():
    pass

def test_table_from_dict():
    pass

def test_table_get_tasks():
    pass

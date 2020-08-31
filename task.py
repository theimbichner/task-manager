import uuid

import task_data
import task_storage

# TODO restrict user property names. Can't be:
# $duration
# task_data.DATE_TYPE_FIELD
# Name
# Date Created
# Date Last Modified
# TODO complete this list
# also include fields from generators, as well as backend names of system fields

class Task:
    fieldMapping = {
        'id': 'id',
        'name': 'name',
        'date_created': 'dateCreated',
        'date_last_modified': 'dateLastModified',
        'markup': 'markup',
        'table_id': 'tableId',
        'properties': 'properties',
        'generator_id': 'generatorId'
    }

    def __init__(self, name, table):
        self.id = str(uuid.uuid4())
        self.name = name
        self.date_created = task_data.DateTime()
        self.date_last_modified = self.date_created
        self.markup = None
        self.table_id = table.id
        self.properties = table.new_default_properties()
        self.generator_id = None

    def to_dict(self):
        data = {}
        set_dict_from_object(self, data, Task.fieldMapping)
        return data

    @staticmethod
    def from_dict(data):
        result = Task(None, None)
        set_object_from_dict(result, data, Task.fieldMapping)
        return result

    def sever_generator(self):
        if self.generator_id is None:
            return
        generator = task_storage.get_generator_by_id(self.generator_id)
        generator.tasks.remove(self.id)
        self.generator_id = None

    # TODO refactor if/else ladder
    # TODO refactor so that system fields dont shadow properties
    def modify(self, remove_generator=True, **kwargs):
        for key in kwargs:
            if key == 'name':
                self.name = kwargs[key]
            else if key == 'markup':
                self.markup = kwargs[key]
            else if key == '$duration' and self.generator_id is not None:
                generator = task_storage.get_generator_by_id(self.generator_id)
                date = self.properties[generator.generation_field]
                date.end = date.start + kwargs[key]
            else if key not in self.properties:
                raise KeyError
            else:
                self.properties[key] = kwargs[key]
        if remove_generator:
            self.sever_generator()
        if remove_generator or bool(kwargs):
            self.date_last_modified = task_data.DateTime()

class Generator:
    fieldMapping = {
        'id': 'id',
        'name': 'name',
        'date_created': 'dateCreated',
        'date_last_modified': 'dateLastModified',
        'template_name': 'templateName',
        'template_markup': 'templateMarkup',
        'template_table_id': 'templateTableId',
        'template_properties': 'templateProperties',
        'template_duration': 'templateDuration',
        'generation_last_timestamp': 'generationLastTimestamp',
        'generation_field': 'generationField',
        'generation_date_pattern': 'generationDatePattern',
        'tasks': 'tasks'
    }

    def __init__(self, name, table, field_name, date_pattern):
        self.id = str(uuid.uuid4())
        self.name = name
        self.date_created = task_data.DateTime()
        self.date_last_modified = self.date_created
        self.template_name = name
        self.template_markup = None
        self.template_table_id = table.id
        self.template_properties = table.new_default_properties()
        self.template_duration = 0
        self.generation_last_timestamp = self.date_created
        self.generation_field = field_name
        self.generation_date_pattern = date_pattern
        self.tasks = []

    def to_dict(self):
        data = {}
        set_dict_from_object(self, data, Generator.fieldMapping)
        return data

    @staticmethod
    def from_dict(data):
        result = Generator(None, None, None, None)
        set_object_from_dict(result, data, Generator.fieldMapping)
        return result

    def generate_tasks(self, timestamp):
        if timestamp <= self.generation_last_timestamp:
            return []
        start_times = self.generation_date_pattern.get_dates(
            self.generation_last_timestamp,
            timestamp)
        new_tasks = [new_task(t) for t in start_times]
        self.tasks += new_tasks
        self.generation_last_timestamp = timestamp
        return new_tasks

    def new_task(self, start_time):
        name = self.template_name
        table = task_storage.get_table_by_id(self.template_table_id)
        result = Task(name, table)
        result.generator_id = self.id
        result.markup = self.template_markup
        result.properties = self.template_properties.copy()
        date_time = task_data.DateTime(start_time, start_time + self.template_duration)
        result.properties[self.generation_field] = date_time
        return result

    # TODO refactor if/else ladder
    # TODO refactor so that system fields dont shadow properties
    def modify(self, **kwargs):
        task_deltas = {}
        for key in kwargs:
            if key == 'name':
                self.name = kwargs[key]
            else if key == 'template_name':
                self.template_name = kwargs[key]
                task_deltas['name'] = kwargs[key]
            else if key == 'template_markup':
                self.template_markup = kwargs[key]
                task_deltas['markup'] = kwargs[key]
            else if key == 'template_duration':
                self.template_duration = kwargs[key]
                task_deltas['$duration'] = kwargs[key]
            else if key == self.generation_field or key not in self.template_properties:
                raise KeyError
            else:
                self.template_properties[key] = kwargs[key]
                task_deltas[key] = kwargs[key]
        if bool(kwargs):
            for t in tasks:
                t.modify(False, **task_deltas)
            self.date_last_modified = task_data.DateTime()

    # TODO verify tasks are stored in the generator in order
    def modify_tasks_following(self, task_id, **kwargs):
        while self.tasks[0] != task_id:
            task = task_storage.get_task_by_id(self.tasks[0])
            task.sever_generator()
        self.modify(**kwargs)

# TODO should a table also count as a task?
class Table:
    fieldMapping = {
        'id': 'id',
        'name': 'name',
        'date_created': 'dateCreated',
        'date_last_modified': 'dateLastModified',
        'tasks': 'tasks',
        'generators': 'generators',
        'schema': 'schema'
    }

    def __init__(self, name, schema):
        self.id = str(uuid.uuid4())
        self.name = name
        self.date_created = task_data.DateTime()
        self.date_last_modified = self.date_created
        tasks = []
        generators = []
        schema = schema

    def to_dict(self):
        data = {}
        set_dict_from_object(self, data, Table.fieldMapping)
        return data

    @staticmethod
    def from_dict(data):
        result = Table(None, None)
        set_object_from_dict(result, data, Table.fieldMapping)
        return result

    def new_default_properties(self):
        properties = {}
        for key in schema:
            properties[key] = schema[key].new_default_value()
        return properties

    def get_tasks(self, timestamp):
        for g in self.generators:
            self.tasks += g.generate_tasks(timestamp)
        return self.tasks.copy()

def set_object_from_dict(obj, data, mapping):
    for key in mapping:
        setattr(obj, key, data[mapping[key]])

def set_dict_from_object(obj, data, mapping):
    for key in mapping:
        data[mapping[key]] = getattr(obj, key)

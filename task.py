import uuid

import task_data
import task_storage

# TODO restrict user property names. Can't be:
# task_data.DATE_TYPE_FIELD
# Name
# Date Created
# Date Last Modified

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
        self.template_name = None
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
        name = self.name if self.template_name is None else self.template_name
        table = task_storage.get_table_by_id(self.template_table_id)
        result = Task(name, table)
        result.generator_id = self.id
        result.markup = self.template_markup
        result.properties = self.template_properties.copy()
        date_time = task_data.DateTime(start_time, start_time + self.template_duration)
        result.properties[self.generation_field] = date_time
        return result

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

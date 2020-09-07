import uuid

import src.task_data as task_data
import src.task_storage as task_storage

# TODO restrict user property names. Can't be:
# task_data.DATE_TYPE_FIELD
# Name
# Date Created
# Date Last Modified

class Task:
    def __init__(self, name, table):
        self.id = str(uuid.uuid4())
        self.name = name
        self.date_created = task_data.DateTime()
        self.date_last_modified = self.date_created
        self.markup = None
        self.table_id = table.id
        self.properties = table._new_default_properties()
        self.generator_id = None

    def to_dict(self):
        data = {}
        _set_dict_from_object(self, data, Task.__field_mapping)
        return data

    @staticmethod
    def from_dict(data):
        result = Task(None, None)
        _set_object_from_dict(result, data, Task.__field_mapping)
        return result

    def modify(self, delta):
        self._modify(True, delta)

    # TODO verify tasks are stored in the generator in order
    def modify_following(self, delta):
        if self.generator_id is None:
            return
        generator = task_storage.get_generator_by_id(self.generator_id)
        while generator.tasks[0] != self.id:
            other_task = task_storage.get_task_by_id(generator.tasks[0])
            other_task._sever_generator()
        generator.modify(delta)

    def _modify(self, remove_generator, delta):
        for key in delta.properties:
            if key in self.properties:
                self.properties[key] = delta.properties[key]
            else:
                raise KeyError
        for key in delta.fields:
            if key in {'name', 'markup'}:
                setattr(self, key, delta.fields[key])
            elif key == 'duration' and self.generator_id is not None:
                generator = task_storage.get_generator_by_id(self.generator_id)
                field = generator.generation_field
                date = self.properties[field]
                self.properties[field] = date.with_duration(delta.fields[key])
            else:
                raise KeyError
        if remove_generator:
            self._sever_generator()
        if remove_generator or delta.is_real_delta():
            self.date_last_modified = task_data.DateTime()

    # TODO should _sever_generator update the modification timestamp when
    # modifying future tasks of a generator?
    def _sever_generator(self):
        if self.generator_id is None:
            return
        generator = task_storage.get_generator_by_id(self.generator_id)
        generator.tasks.remove(self.id)
        self.generator_id = None

    __field_mapping = {
        'id': 'id',
        'name': 'name',
        'date_created': 'dateCreated',
        'date_last_modified': 'dateLastModified',
        'markup': 'markup',
        'table_id': 'tableId',
        'properties': 'properties',
        'generator_id': 'generatorId'
    }

class Generator:
    def __init__(self, name, table, field_name, date_pattern):
        self.id = str(uuid.uuid4())
        self.name = name
        self.date_created = task_data.DateTime()
        self.date_last_modified = self.date_created
        self.template_name = name
        self.template_markup = None
        self.template_table_id = table.id
        self.template_properties = table._new_default_properties()
        self.template_duration = 0
        self.generation_last_timestamp = self.date_created
        self.generation_field = field_name
        self.generation_date_pattern = date_pattern
        self.tasks = []

    def to_dict(self):
        data = {}
        _set_dict_from_object(self, data, Generator.__field_mapping)
        return data

    @staticmethod
    def from_dict(data):
        result = Generator(None, None, None, None)
        _set_object_from_dict(result, data, Generator.__field_mapping)
        return result

    def modify(self, delta):
        for key in delta.properties:
            if key in self.template_properties and key != self.generation_field:
                self.template_properties[key] = delta.properties[key]
            else:
                raise KeyError
        for key in delta.fields:
            if key in {'name', 'template_name', 'template_markup', 'template_duration'}:
                setattr(self, key, delta.fields[key])
            else:
                raise KeyError
        if delta.is_real_delta():
            instance_delta = delta.to_instance_delta()
            for t in self.tasks:
                t._modify(False, instance_delta)
            self.date_last_modified = task_data.DateTime()

    def _generate_tasks(self, timestamp):
        if timestamp <= self.generation_last_timestamp:
            return []
        start_times = self.generation_date_pattern.get_dates(
            self.generation_last_timestamp,
            timestamp)
        new_tasks = [__new_task(t) for t in start_times]
        self.tasks += new_tasks
        self.generation_last_timestamp = timestamp
        return new_tasks

    def __new_task(self, start_time):
        table = task_storage.get_table_by_id(self.template_table_id)
        result = Task(self.template_name, table)
        result.generator_id = self.id
        result.markup = self.template_markup
        result.properties = self.template_properties.copy()
        date_time = task_data.DateTime(start_time, start_time + self.template_duration)
        result.properties[self.generation_field] = date_time
        return result

    __field_mapping = {
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

# TODO should a table also count as a task?
class Table:
    def __init__(self, name, schema):
        self.id = str(uuid.uuid4())
        self.name = name
        self.date_created = task_data.DateTime()
        self.date_last_modified = self.date_created
        self.tasks = []
        self.generators = []
        self.schema = schema

    def to_dict(self):
        data = {}
        _set_dict_from_object(self, data, Table.__field_mapping)
        return data

    @staticmethod
    def from_dict(data):
        result = Table(None, None)
        _set_object_from_dict(result, data, Table.__field_mapping)
        return result

    def get_tasks(self, timestamp):
        for g in self.generators:
            self.tasks += g._generate_tasks(timestamp)
        return self.tasks.copy()

    def _new_default_properties(self):
        properties = {}
        for key in self.schema:
            properties[key] = self.schema[key].new_default_value()
        return properties

    __field_mapping = {
        'id': 'id',
        'name': 'name',
        'date_created': 'dateCreated',
        'date_last_modified': 'dateLastModified',
        'tasks': 'tasks',
        'generators': 'generators',
        'schema': 'schema'
    }

class Delta:
    def __init__(self, fields, properties):
        self.fields = fields
        self.properties = properties

    def with_fields(self, **kwargs):
        self.fields.update(kwargs)
        return self

    def with_properties(self, **kwargs):
        self.properties.update(kwargs)
        return self

    def to_instance_delta(self):
        results = Delta(self.fields.copy(), self.properties.copy())
        for key in ['template_name', 'template_markup', 'template_duration']:
            if key in results.fields:
                replaced = key.replace('template_', '', 1)
                results.fields[replaced] = results.fields[key]
                del results.fields[key]
        return results

    def is_real_delta(self):
        return bool(self.fields) or bool(self.properties)

def _set_object_from_dict(obj, data, mapping):
    for key in mapping:
        setattr(obj, key, data[mapping[key]])

def _set_dict_from_object(obj, data, mapping):
    for key in mapping:
        data[mapping[key]] = getattr(obj, key)

import time

import pytest

import src.task_time as task_time

time_start = 123456
time_end = 456123

def test_date_time_init():
    start = int(time.time())
    date_time = task_time.DateTime()
    end = int(time.time())

    assert date_time.start >= start
    assert date_time.start <= end
    assert date_time.end == date_time.start

def test_date_time_start_init():
    date_time = task_time.DateTime(time_start)

    assert date_time.start == time_start
    assert date_time.end == date_time.start

def test_date_time_start_end_init():
    date_time = task_time.DateTime(time_start, time_end)

    assert date_time.start == time_start
    assert date_time.end == time_end

def test_date_time_eq():
    date_time_1 = task_time.DateTime(time_start, time_end)
    date_time_2 = task_time.DateTime(time_start, time_end)

    assert date_time_1 == date_time_2
    assert not (date_time_1 != date_time_2)

neq_date_times = [
    task_time.DateTime(time_start - 1, time_end),
    task_time.DateTime(time_start, time_end + 1),
    task_time.DateTime(time_start - 1, time_end + 1),
    'datetime',
    None
]

@pytest.mark.parametrize('comparison', neq_date_times)
def test_date_time_not_eq(comparison):
    date_time = task_time.DateTime(time_start, time_end)

    assert not (date_time == comparison)
    assert date_time != comparison

def test_date_time_with_duration():
    duration = 1000
    date_time = task_time.DateTime(time_start, time_end)
    new_date_time = date_time.with_duration(duration)

    assert new_date_time.start == time_start
    assert new_date_time.end == time_start + duration
    assert date_time.end == time_end

def test_date_time_to_from_dict():
    date_time = task_time.DateTime(time_start, time_end)
    data = date_time.to_dict()
    new_date_time = task_time.DateTime.from_dict(data)

    assert date_time == new_date_time

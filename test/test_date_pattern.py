import pytest

import src.date_pattern as date_pattern

example_union = date_pattern.UnionDatePattern([
    date_pattern.UniformDatePattern(75, 7),
    date_pattern.UniformDatePattern(93, 1),
    date_pattern.UniformDatePattern(6, 24),
    date_pattern.UniformDatePattern(89, 4)
])

def test_uniform_to_from_dict():
    pattern = date_pattern.UniformDatePattern(11, 13)
    data = pattern.to_dict()
    new_pattern = date_pattern.UniformDatePattern.from_dict(data)
    assert pattern.get_dates(0, 1000) == new_pattern.get_dates(0, 1000)

def test_union_to_from_dict():
    pattern = example_union
    data = pattern.to_dict()
    new_pattern = date_pattern.UnionDatePattern.from_dict(data)
    assert pattern.get_dates(0, 1000) == new_pattern.get_dates(0, 1000)

get_dates_data = [
    # UnionDatePattern
    (example_union, 0, 100, [6, 30, 54, 75, 78, 82, 89, 93, 94, 95, 96, 97, 98, 99, 100]),

    # UniformDatePattern

    # result lies on start
    (date_pattern.UniformDatePattern(10, 4), 14, 28, [18, 22, 26]),

    # result lies on end
    (date_pattern.UniformDatePattern(10, 4), 16, 30, [18, 22, 26, 30]),

    # init < start (difference multiple of delta)
    (date_pattern.UniformDatePattern(10, 4), 18, 29, [22, 26]),

    # init < start (difference not a multiple)
    (date_pattern.UniformDatePattern(10, 4), 11, 18, [14, 18]),

    # init == start
    (date_pattern.UniformDatePattern(10, 4), 10, 20, [14, 18]),

    # start < init < end
    (date_pattern.UniformDatePattern(10, 4), 8, 19, [10, 14, 18]),

    # init == end
    (date_pattern.UniformDatePattern(10, 4), 3, 10, [10]),

    # end < init
    (date_pattern.UniformDatePattern(10, 4), 1, 5, []),

    # empty result (hop over)
    (date_pattern.UniformDatePattern(10, 4), 11, 13, []),

    # one result (init)
    (date_pattern.UniformDatePattern(10, 4), 8, 12, [10]),

    # one result (not init)
    (date_pattern.UniformDatePattern(10, 4), 13, 15, [14])
]

@pytest.mark.parametrize('pattern, start, end, expected', get_dates_data)
def test_get_dates(pattern, start, end, expected):
    assert pattern.get_dates(start, end) == expected

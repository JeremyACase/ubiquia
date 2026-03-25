import pytest
from pydantic import ValidationError

from util_simulation_service.model.time_offset import TimeOffset, TimeUnit


class TestTimeOffset:
    def test_defaults_to_seconds(self):
        offset = TimeOffset(n=5.0)
        assert offset.unit == TimeUnit.SECONDS

    def test_accepts_minutes(self):
        offset = TimeOffset(n=1.0, unit=TimeUnit.MINUTES)
        assert offset.unit == TimeUnit.MINUTES

    def test_accepts_hours(self):
        offset = TimeOffset(n=2.0, unit=TimeUnit.HOURS)
        assert offset.unit == TimeUnit.HOURS

    def test_n_must_be_positive(self):
        with pytest.raises(ValidationError):
            TimeOffset(n=0)

    def test_n_must_be_positive_negative(self):
        with pytest.raises(ValidationError):
            TimeOffset(n=-1.0)

    def test_n_accepts_float(self):
        offset = TimeOffset(n=0.5)
        assert offset.n == 0.5


class TestTimeOffsetToSeconds:
    def test_seconds_unit(self):
        assert TimeOffset(n=30.0, unit=TimeUnit.SECONDS).to_seconds() == 30.0

    def test_minutes_unit(self):
        assert TimeOffset(n=2.0, unit=TimeUnit.MINUTES).to_seconds() == 120.0

    def test_hours_unit(self):
        assert TimeOffset(n=1.5, unit=TimeUnit.HOURS).to_seconds() == 5400.0

    def test_default_unit_is_seconds(self):
        assert TimeOffset(n=10.0).to_seconds() == 10.0

    def test_fractional_minutes(self):
        assert TimeOffset(n=0.5, unit=TimeUnit.MINUTES).to_seconds() == 30.0

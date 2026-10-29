def test_cronjob_config_file_exists():
    from pathlib import Path
    assert Path(__file__).resolve().parents[1].joinpath("cronjobVA.yaml").is_file()

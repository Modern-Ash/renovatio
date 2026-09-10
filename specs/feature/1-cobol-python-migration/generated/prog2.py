# Generated from COBOL program: prog2
from decimal import Decimal


def run_prog2(input_record):
    """Simple generated function for program prog2
    Expects input_record as a dict with keys matching field names.
    """
    # Example mapping from COBOL fields to Python variables
    result = {}

    # field: FIELD1 (picture: X(5))
    result['FIELD1'] = input_record.get('FIELD1')

    # field: FIELD2 (picture: 9(5))
    result['FIELD2'] = input_record.get('FIELD2')

    # field: FIELD1B (picture: X(3))
    result['FIELD1B'] = input_record.get('FIELD1B')

    return result


if __name__ == '__main__':
    sample = { 'FIELD1': None, 'FIELD2': None, 'FIELD1B': None }
    print(run_prog2(sample))
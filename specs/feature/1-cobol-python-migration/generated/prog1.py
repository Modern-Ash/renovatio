# Generated from COBOL program: prog1
from decimal import Decimal


def run_prog1(input_record):
    """Simple generated function for program prog1
    Expects input_record as a dict with keys matching field names.
    """
    # Example mapping from COBOL fields to Python variables
    result = {}

    # field: FIELD1 (picture: X(10))
    result['FIELD1'] = input_record.get('FIELD1')

    # field: AMOUNT (picture: 9(7)V99)
    result['AMOUNT'] = input_record.get('AMOUNT')

    return result


if __name__ == '__main__':
    sample = { 'FIELD1': None, 'AMOUNT': None }
    print(run_prog1(sample))
// generated-by: generate_node_unit_tests.py
describe('../../../src/utils/mdmsData', () => {
  test('module_whenRequired_loadsWithoutThrow', () => {
    jest.resetModules();
    let mod;
    expect(() => {
      mod = require('../../../src/utils/mdmsData');
    }).not.toThrow();
    expect(mod).toBeDefined();
    
  });
});

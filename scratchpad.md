setting up cosmos emulator in Github CI for integration tests
https://learn.microsoft.com/en-us/azure/cosmos-db/how-to-develop-emulator?tabs=docker-linux%2Ccsharp&pivots=api-nosql#use-the-emulator-in-a-github-actions-ci-workflow


|                                      | Operations                                                                                                                       | H (HEAD) | T (TAIL) | C (CURSOR) |
|--------------------------------------|----------------------------------------------------------------------------------------------------------------------------------|----------|----------|------------|
| On initialization                    | peekPrev & peekNext returns null<br/> readPrev & readNext return null and dont move cursor<br/>offerNext & offerPrev permissible | -1       | -1       | -1         |
| First offerNext after initialization | peekNext & readNext returns null<br/>peekPrev returns A[0]<br/>readPrev returns A[0] and moves cursor                            | 0        | 0        | 1          |
| readPrev thereafter                  |                                                                                                                                  | 0        | 0        | 0          |
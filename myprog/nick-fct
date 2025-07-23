def thrice(f):
  def h(x):
    return f(f(f(x)))
  return h

def inc(x):
  return x + 1

x = 4
thrice(thrice)(inc)(x)

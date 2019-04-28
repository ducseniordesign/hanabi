#include <iostream>
#include <fstream>

using namespace std;

int main() {
  ifstream output("output.txt");
  ifstream output2("output2.txt");
  int count;
  for (int i = 0; i < 1735; i++) {
    char o, o2;
    output >> o;
    output2 >> o2;
    if (o != o2) {
      cout << "Difference found at location: " << i << endl;
      cout << o << ' ' << o2 << endl;
      count++;
    }
  }
  cout << "Total difference: " << count << endl;
}

package com.silentlexx.ffmpeggui_os.activities.widgets.parser;

public interface ParserSerializable {
  String getName(int position);
  String getArg(int position);
  ParserType getType(int position);
  String[] getNames();
  void removeByArg(String arg);
  int findPositionByArg(String arg);
  void remove(int position);

}
